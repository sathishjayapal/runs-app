package me.sathish.runs_app.file_name_tracker;

import static org.junit.jupiter.api.Assertions.*;

import me.sathish.runs_app.config.BaseIT;
import me.sathish.runs_app.run_app_user.RunAppUser;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Integration tests for FileNameTracker functionality.
 * Tests duplicate file detection and file tracking per user.
 */
@Sql("/data/fileTrackerData.sql")
public class FileNameTrackerServiceTest extends BaseIT {

    @Autowired
    private FileNameTrackerService fileNameTrackerService;

    @Test
    @Transactional
    public void trackFile_newFile_success() {
        RunAppUser user = runAppUserRepository.findById(10004L).orElseThrow();
        String fileName = "new-run-2026-02-20.fit";

        // Track new file
        FileNameTracker tracker = new FileNameTracker();
        tracker.setFileName(fileName);
        tracker.setCreatedBy(user);

        FileNameTracker saved = fileNameTrackerRepository.save(tracker);

        assertNotNull(saved.getId());
        assertEquals(fileName, saved.getFileName());
        assertEquals(user.getId(), saved.getCreatedBy().getId());
    }

    @Test
    public void isFileProcessed_existingFile_returnsTrue() {
        // File exists in database
        boolean processed = fileNameTrackerRepository.existsByFileName(
                "morning-run-2026-02-01.fit"
        );

        assertTrue(processed);
    }

    @Test
    public void isFileProcessed_newFile_returnsFalse() {
        // File does not exist in database
        boolean processed = fileNameTrackerRepository.existsByFileName(
                "brand-new-file.fit"
        );

        assertFalse(processed);
    }

    @Test
    public void findByFileName_existingFile_returnsFile() {
        // File exists in database
        FileNameTracker tracker = fileNameTrackerRepository.findByFileName(
                "morning-run-2026-02-01.fit"
        );

        assertNotNull(tracker);
        assertEquals("morning-run-2026-02-01.fit", tracker.getFileName());
    }

    @Test
    public void findByCreatedBy_returnsUserFiles() {
        RunAppUser user = runAppUserRepository.findById(10004L).orElseThrow();

        List<FileNameTracker> files = fileNameTrackerRepository.findByCreatedBy(user);

        assertEquals(3, files.size());
        assertTrue(files.stream()
                .allMatch(f -> f.getCreatedBy().getId().equals(10004L)));
    }

    @Test
    public void findByCreatedBy_differentUser_returnsDifferentFiles() {
        RunAppUser user1 = runAppUserRepository.findById(10004L).orElseThrow();
        RunAppUser user2 = runAppUserRepository.findById(10005L).orElseThrow();

        List<FileNameTracker> user1Files = fileNameTrackerRepository.findByCreatedBy(user1);
        List<FileNameTracker> user2Files = fileNameTrackerRepository.findByCreatedBy(user2);

        assertEquals(3, user1Files.size());
        assertEquals(2, user2Files.size());

        // Verify no overlap
        List<String> user1FileNames = user1Files.stream()
                .map(FileNameTracker::getFileName)
                .toList();
        List<String> user2FileNames = user2Files.stream()
                .map(FileNameTracker::getFileName)
                .toList();

        assertTrue(user1FileNames.stream().noneMatch(user2FileNames::contains));
    }

    @Test
    @Transactional
    public void trackDuplicateFile_sameUser_violatesConstraint() {
        RunAppUser user = runAppUserRepository.findById(10004L).orElseThrow();
        String existingFileName = "morning-run-2026-02-01.fit";

        // Attempt to track duplicate file for same user
        FileNameTracker duplicate = new FileNameTracker();
        duplicate.setFileName(existingFileName);
        duplicate.setCreatedBy(user);

        // This should either throw exception or be prevented by unique constraint
        assertThrows(Exception.class, () -> {
            fileNameTrackerRepository.saveAndFlush(duplicate);
        });
    }

    @Test
    @Transactional
    public void trackDuplicateFile_differentUser_success() {
        RunAppUser user1 = runAppUserRepository.findById(10004L).orElseThrow();
        RunAppUser user2 = runAppUserRepository.findById(10005L).orElseThrow();

        String fileName = "shared-file-name.fit";

        // User 1 tracks file
        FileNameTracker tracker1 = new FileNameTracker();
        tracker1.setFileName(fileName);
        tracker1.setCreatedBy(user1);
        fileNameTrackerRepository.save(tracker1);

        // User 2 can track same filename (different user)
        FileNameTracker tracker2 = new FileNameTracker();
        tracker2.setFileName(fileName);
        tracker2.setCreatedBy(user2);
        FileNameTracker saved = fileNameTrackerRepository.save(tracker2);

        assertNotNull(saved.getId());
        assertEquals(fileName, saved.getFileName());
        assertEquals(user2.getId(), saved.getCreatedBy().getId());
    }

    @Test
    public void countByCreatedBy_returnsCorrectCount() {
        RunAppUser user = runAppUserRepository.findById(10004L).orElseThrow();

        long count = fileNameTrackerRepository.countByCreatedBy(user);

        assertEquals(3, count);
    }

    @Test
    @Transactional
    public void deleteTrackedFile_success() {
        RunAppUser user = runAppUserRepository.findById(10004L).orElseThrow();

        long initialCount = fileNameTrackerRepository.countByCreatedBy(user);

        // Delete one file
        fileNameTrackerRepository.deleteById(30001L);
        fileNameTrackerRepository.flush();

        long finalCount = fileNameTrackerRepository.countByCreatedBy(user);

        assertEquals(initialCount - 1, finalCount);
    }

    @Test
    public void findAll_returnsAllTrackedFiles() {
        List<FileNameTracker> allFiles = fileNameTrackerRepository.findAll();

        assertEquals(5, allFiles.size());
    }

    @Test
    @Transactional
    public void updateTrackedFile_success() {
        FileNameTracker tracker = fileNameTrackerRepository.findById(30001L).orElseThrow();

        String originalFileName = tracker.getFileName();
        assertNotNull(originalFileName);

        // Update with user who can update
        RunAppUser updateUser = runAppUserRepository.findById(10004L).orElseThrow();
        tracker.setUpdatedBy(updateUser);

        FileNameTracker updated = fileNameTrackerRepository.save(tracker);

        assertNotNull(updated.getUpdatedBy());
        assertEquals(10004L, updated.getUpdatedBy().getId());
    }

    @Test
    public void testRepositoryDirectAccess() {
        // Verify repository is properly autowired and functional
        assertEquals(5, fileNameTrackerRepository.count());

        // Test findById
        var tracker = fileNameTrackerRepository.findById(30001L);
        assertTrue(tracker.isPresent());
        assertEquals("morning-run-2026-02-01.fit", tracker.get().getFileName());
        assertEquals(10004L, tracker.get().getCreatedBy().getId());
    }

    @Test
    public void testFileNamePattern_fitExtension() {
        RunAppUser user = runAppUserRepository.findById(10004L).orElseThrow();

        List<FileNameTracker> files = fileNameTrackerRepository.findByCreatedBy(user);

        // All tracked files should have .fit extension
        assertTrue(files.stream()
                .allMatch(f -> f.getFileName().endsWith(".fit")));
    }

    @Test
    @Transactional
    public void preventDuplicateImport_workflow() {
        RunAppUser user = runAppUserRepository.findById(10005L).orElseThrow();
        String fileName = "test-import.fit";

        // Check if file already processed
        boolean alreadyProcessed = fileNameTrackerRepository
                .existsByFileName(fileName);
        assertFalse(alreadyProcessed, "File should not be processed yet");

        // Process file and track it
        FileNameTracker tracker = new FileNameTracker();
        tracker.setFileName(fileName);
        tracker.setCreatedBy(user);
        fileNameTrackerRepository.save(tracker);

        // Try to process same file again
        boolean nowProcessed = fileNameTrackerRepository
                .existsByFileName(fileName);
        assertTrue(nowProcessed, "File should now be marked as processed");

        // Duplicate should be prevented
        assertThrows(Exception.class, () -> {
            FileNameTracker duplicate = new FileNameTracker();
            duplicate.setFileName(fileName);
            duplicate.setCreatedBy(user);
            fileNameTrackerRepository.saveAndFlush(duplicate);
        });
    }
}
