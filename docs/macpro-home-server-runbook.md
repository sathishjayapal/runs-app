# From VirtualBox to Bare Metal: 2013 Mac Pro Home Server Runbook

This is the practical runbook behind converting a 2013 Mac Pro (MacPro6,1) from a macOS + VirtualBox host into a bare-metal Ubuntu Server machine for the Runs application stack.

The goal was not to build a virtualization lab. The goal was to remove layers, make the host predictable after reboot, and provide a stable place for Docker, RabbitMQ, PostgreSQL, Portainer, and the Runs application.

> **Warning:** Several steps below are destructive. Device names such as `/dev/disk2` and IP addresses are examples from one machine and can change. Always identify the target device before erasing or writing to it.

## 1. Inventory before changing anything

On macOS:

```bash
sw_vers
system_profiler SPHardwareDataType
system_profiler SPStorageDataType
diskutil list

VBoxManage list vms
VBoxManage list runningvms
VBoxManage showvminfo "LinuxForClaude"
VBoxManage list hdds
```

The machine used for this build was a MacPro6,1 with a quad-core Intel Xeon E5, 32 GB RAM, and an OWC Aura Pro X2 internal SSD.

## 2. Verify the backup instead of assuming it exists

The original Time Machine history turned out to be stale, so a new recovery point was created before erasing macOS.

```bash
tmutil status
tmutil latestbackup
tmutil listbackups | tail -5
tmutil destinationinfo
```

Verify any external data volume that you intend to preserve:

```bash
diskutil verifyVolume "/Volumes/MacProHD"
```

A healthy result should end with something similar to:

```text
The volume ... appears to be OK
File system check exit code is 0
```

After completing a fresh Time Machine backup, verify again:

```bash
tmutil status
tmutil latestbackup
tmutil destinationinfo
```

Before installing Linux, physically disconnect the backup disk. That removes the possibility of selecting it accidentally in the installer.

## 3. Create the Ubuntu installer

For this Intel Mac Pro, use the **AMD64** Ubuntu Server image, not ARM64.

Example image used for this build:

```text
ubuntu-24.04.4-live-server-amd64.iso
```

After flashing the image to a USB drive, macOS may no longer show the USB as a normal Finder volume. Confirm the physical device instead:

```bash
diskutil list external physical
```

A successfully flashed Linux installer typically shows multiple partitions instead of a single normal FAT32 data partition.

On an Intel Mac, boot while holding **Option**. On a Windows USB keyboard, **Alt** acts as Option. Select **EFI Boot**.

## 4. Ubuntu Server installation choices

Choices used for this home server:

- Ubuntu Server standard install, not minimized
- No HTTP proxy
- Default Ubuntu mirror
- Install OpenSSH server
- Allow password authentication initially so first remote login is simple
- Skip Ubuntu Pro during installation
- Skip featured server snaps
- Do not install Docker from the Ubuntu snap selection screen

### Storage layout

The internal SSD was approximately 894 GiB as reported by Linux.

Guided LVM was used, but the default 100 GB root logical volume was enlarged before committing the installation:

```text
EFI               ~1 GB
/boot             ~2 GB
LVM volume group  ~891 GB
  /               ~500 GB
  free in VG      ~390 GB
```

The free LVM capacity is intentional. It can later become a dedicated `/srv`, database, or other data filesystem.

Before confirming the destructive storage step, verify that only the internal SSD and the installer USB are visible and that the external backup disk is disconnected.

## 5. First-boot acceptance test

Before installing application software, prove the OS is healthy:

```bash
hostnamectl
ip -br addr
free -h
df -h
systemctl --failed
```

Update and reboot:

```bash
sudo apt update
sudo apt upgrade -y
sudo reboot
```

Reconnect over SSH and verify again:

```bash
hostname
uptime
systemctl --failed
free -h
df -h /
```

The acceptance criteria for this build were:

```text
hostname           runs-server
failed units       0
RAM                 ~31 GiB usable
root filesystem     ~492 GB
SSH                 working after reboot
```

## 6. See the LVM capacity that `df` does not show

`df -h` only reports mounted filesystems. To see the unused space still available inside LVM:

```bash
lsblk
sudo pvs
sudo vgs
sudo lvs
```

This is useful when the root filesystem is intentionally smaller than the entire SSD.

## 7. Install Docker Engine from Docker's official repository

Install prerequisites:

```bash
sudo apt update
sudo apt install -y ca-certificates curl
```

Add Docker's signing key:

```bash
sudo install -m 0755 -d /etc/apt/keyrings
sudo curl -fsSL https://download.docker.com/linux/ubuntu/gpg \
  -o /etc/apt/keyrings/docker.asc
sudo chmod a+r /etc/apt/keyrings/docker.asc
```

Add Docker's apt repository:

```bash
sudo tee /etc/apt/sources.list.d/docker.sources <<EOF
Types: deb
URIs: https://download.docker.com/linux/ubuntu
Suites: $(. /etc/os-release && echo "${UBUNTU_CODENAME:-$VERSION_CODENAME}")
Components: stable
Architectures: $(dpkg --print-architecture)
Signed-By: /etc/apt/keyrings/docker.asc
EOF

sudo apt update
```

Install Docker Engine, Buildx, and Compose:

```bash
sudo apt install -y \
  docker-ce \
  docker-ce-cli \
  containerd.io \
  docker-buildx-plugin \
  docker-compose-plugin
```

Verify:

```bash
sudo systemctl status docker --no-pager
sudo docker run --rm hello-world
docker compose version
```

Enable normal-user Docker access for the trusted admin account:

```bash
sudo usermod -aG docker $USER
```

Log out and back in before testing:

```bash
docker ps
docker run --rm hello-world
```

Also confirm Docker will return after reboot:

```bash
sudo systemctl enable docker.service
sudo systemctl enable containerd.service
sudo systemctl is-enabled docker
sudo systemctl is-enabled containerd
```

> Membership in the `docker` group effectively grants root-level control of the host. Only trusted administrator accounts should be added.

## 8. Create a predictable application directory

```bash
sudo mkdir -p /opt/runs/platform
sudo chown -R $USER:$USER /opt/runs
cd /opt/runs/platform
```

## 9. RabbitMQ + Portainer platform stack

Generate RabbitMQ credentials without hardcoding a password into Compose:

```bash
umask 077
RABBIT_PASS=$(openssl rand -hex 24)

cat > .env <<EOF
RABBITMQ_DEFAULT_USER=runs
RABBITMQ_DEFAULT_PASS=$RABBIT_PASS
EOF

chmod 600 .env
```

Create `compose.yaml`:

```yaml
services:
  rabbitmq:
    image: rabbitmq:4.3.5-management
    container_name: rabbitmq
    hostname: rabbitmq
    restart: unless-stopped
    environment:
      RABBITMQ_DEFAULT_USER: ${RABBITMQ_DEFAULT_USER}
      RABBITMQ_DEFAULT_PASS: ${RABBITMQ_DEFAULT_PASS}
    ports:
      - "5672:5672"
      - "15672:15672"
    volumes:
      - rabbitmq_data:/var/lib/rabbitmq
    healthcheck:
      test: ["CMD", "rabbitmq-diagnostics", "-q", "ping"]
      interval: 30s
      timeout: 10s
      retries: 5
      start_period: 20s

  portainer:
    image: portainer/portainer-ce:lts
    container_name: portainer
    restart: unless-stopped
    ports:
      - "9443:9443"
    volumes:
      - /var/run/docker.sock:/var/run/docker.sock
      - portainer_data:/data

volumes:
  rabbitmq_data:
  portainer_data:
```

Validate and start:

```bash
docker compose config
docker compose up -d
docker ps
```

Check RabbitMQ health:

```bash
docker inspect rabbitmq \
  --format='{{.State.Status}} / {{if .State.Health}}{{.State.Health.Status}}{{end}}'
```

Expected result after startup:

```text
running / healthy
```

Useful endpoints on the LAN:

```text
RabbitMQ AMQP        tcp://<runs-server-ip>:5672
RabbitMQ Management http://<runs-server-ip>:15672
Portainer            https://<runs-server-ip>:9443
```

Portainer uses a self-signed certificate by default, so a browser warning is expected on first access.

## 10. Quick performance baseline

Install basic benchmark tools while the server is still mostly empty:

```bash
sudo apt update
sudo apt install -y sysbench fio stress-ng lm-sensors smartmontools
```

CPU:

```bash
sysbench cpu \
  --threads=8 \
  --cpu-max-prime=20000 \
  run
```

Memory:

```bash
sysbench memory \
  --threads=8 \
  --memory-total-size=20G \
  run
```

Temporary SSD sequential write/read test:

```bash
fio --name=seqwrite \
  --filename=/tmp/fio-test \
  --size=4G \
  --rw=write \
  --bs=1M \
  --direct=1 \
  --iodepth=16 \
  --numjobs=1

fio --name=seqread \
  --filename=/tmp/fio-test \
  --size=4G \
  --rw=read \
  --bs=1M \
  --direct=1 \
  --iodepth=16 \
  --numjobs=1

rm -f /tmp/fio-test
```

Five-minute CPU stability test:

```bash
stress-ng \
  --cpu 8 \
  --cpu-method all \
  --timeout 5m \
  --metrics-brief
```

Watch temperatures in a second SSH session:

```bash
watch -n 2 sensors
```

After stress testing:

```bash
uptime
systemctl --failed
```

## 11. Lessons from the build

1. **Remove layers before adding technology.** The original stack was hardware → macOS → VirtualBox → Ubuntu → Docker. The workload only needed hardware → Ubuntu → Docker.
2. **A backup is not a backup until it is verified.** Checking `tmutil latestbackup` revealed that the assumed recovery point was stale.
3. **Destructive changes deserve explicit gates.** Verify filesystems, confirm the target disk, disconnect backup media, and validate reboot/SSH before adding applications.
4. **Plan storage deliberately.** LVM made it possible to use a large root filesystem without consuming the entire disk on day one.
5. **Headless does not mean hard to manage.** Once SSH survived an update/reboot cycle, the monitor and keyboard were no longer operational dependencies.
6. **Portainer is an accelerator, not a replacement for Docker knowledge.** Compose remains the source of truth, while Portainer shortens the feedback loop for container status, logs, volumes, and routine operations.
7. **Old hardware can be useful infrastructure when the workload fits.** The value came from matching the workload to the machine rather than chasing newer hardware or a larger cloud design.

## 12. Target architecture

```text
2013 Mac Pro
MacPro6,1
Xeon E5 / 32 GB RAM / ~1 TB SSD
        |
        v
Ubuntu Server 24.04 LTS
        |
        v
Docker Engine + Compose
        |
        +-- Portainer
        +-- RabbitMQ
        +-- PostgreSQL
        +-- Runs App
```

The important part of this build was not any single command. It was moving from a working-but-layered setup to something simpler, recoverable, observable, and easy to operate.