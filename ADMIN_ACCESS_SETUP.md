# Admin-Only Access for RunAppUsers - Implementation Guide

## Summary of Changes

### Backend Changes

1. **Created `/api/current-user` endpoint**
    - File: `src/main/java/me/sathish/runs_app/security/CurrentUserResource.java`
    - Returns current user's username and roles
    - Used by frontend to determine user permissions

2. **Fixed role loading from database**
    - File: `src/main/java/me/sathish/runs_app/security/RunsAppSecurityUserDetailsService.java`
    - Now properly loads roles from `run_app_user_roles` table
    - Falls back to `ROLE_USER` if no roles assigned

3. **Added eager role fetching**
    - File: `src/main/java/me/sathish/runs_app/run_app_user/RunAppUserRepository.java`
    - New method: `findByEmailIgnoreCaseWithRoles()` to avoid lazy loading issues

4. **Fixed RunnerAppRole endpoint permissions**
    - File: `src/main/java/me/sathish/runs_app/runner_app_role/RunnerAppRoleResource.java`
    - GET operations: All authenticated users (needed for dropdown in forms)
    - POST/PUT/DELETE: Admin only

5. **RunAppUser endpoints already protected**
    - File: `src/main/java/me/sathish/runs_app/run_app_user/RunAppUserResource.java`
    - All operations require `ROLE_ADMIN`

### Frontend Changes

1. **Created auth hook**
    - File: `src/main/webapp/app/common/use-auth.ts`
    - Fetches current user info from `/api/current-user`
    - Provides `isAdmin()` function

2. **Created admin route guard**
    - File: `src/main/webapp/app/common/admin-route.tsx`
    - Redirects non-admin users to home page
    - Shows loading state while checking permissions

3. **Protected RunAppUser routes**
    - File: `src/main/webapp/app/routes.tsx`
    - All `/runAppUsers/*` routes wrapped with `<AdminRoute>`

4. **Conditional UI rendering**
    - File: `src/main/webapp/app/common/header.tsx`
    - RunAppUsers link only shows for admins
    - File: `src/main/webapp/app/home/home.tsx`
    - RunAppUsers link only shows for admins

## Database Setup Required

### 1. Ensure roles exist in `runner_app_role` table:

```sql
-- Check if roles exist
SELECT *
FROM runner_app_role;

-- If not, create them:
INSERT INTO runner_app_role (id, role_name, description, created_at, updated_at)
VALUES (1, 'ROLE_ADMIN', 'Administrator role with full access', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
       (2, 'ROLE_USER', 'Regular user role', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);
```

### 2. Assign roles to users in `run_app_user_roles` table:

```sql
-- Check current user-role assignments
SELECT u.email, r.role_name
FROM run_app_user u
         LEFT JOIN run_app_user_roles ur ON u.id = ur.user_id
         LEFT JOIN runner_app_role r ON ur.role_id = r.id;

-- Assign ROLE_ADMIN to admin users (replace with actual user IDs)
INSERT INTO run_app_user_roles (user_id, role_id)
SELECT u.id, 1
FROM run_app_user u
WHERE u.email = 'admin@runsapp.com';

-- Assign ROLE_USER to regular users (replace with actual user IDs)
INSERT INTO run_app_user_roles (user_id, role_id)
SELECT u.id, 2
FROM run_app_user u
WHERE u.email = 'runner@runsapp.com';
```

## Testing Guide

### Test 1: Admin User Access

**Login as:** User with `ROLE_ADMIN` (e.g., admin@runsapp.com)

**Expected behavior:**

- ✅ Can see "Run App Users" link in navigation menu
- ✅ Can see "Run App Users" link on home page
- ✅ Can access `/runAppUsers` page
- ✅ Can add/edit/delete users
- ✅ Can access Garmin Runs, Shedlocks, etc.

**Test steps:**

1. Login with admin credentials
2. Check navigation menu - should see "Run App Users"
3. Click "Run App Users" - should load user list
4. Try to add a new user - should work
5. Access other pages (Garmin Runs, etc.) - should work

### Test 2: Regular User Access

**Login as:** User with `ROLE_USER` only (e.g., runner@runsapp.com)

**Expected behavior:**

- ✅ Cannot see "Run App Users" link in navigation menu
- ✅ Cannot see "Run App Users" link on home page
- ✅ Accessing `/runAppUsers` directly redirects to home
- ✅ Can access Garmin Runs, Shedlocks, etc.

**Test steps:**

1. Login with regular user credentials
2. Check navigation menu - should NOT see "Run App Users"
3. Try to access `/runAppUsers` directly in browser - should redirect to home
4. Access other pages (Garmin Runs, etc.) - should work normally

### Test 3: User with No Roles

**Login as:** User with no roles assigned in database

**Expected behavior:**

- ✅ Automatically gets `ROLE_USER` (fallback)
- ✅ Same behavior as Test 2 (regular user)

## Debugging

### Check current user info:

1. Login to the application
2. Open browser DevTools (F12)
3. Go to Network tab
4. Look for request to `/api/current-user`
5. Check the response - should show:
   ```json
   {
     "username": "your-email@example.com",
     "roles": ["ROLE_ADMIN"]  // or ["ROLE_USER"]
   }
   ```

### Check backend logs:

Look for these log messages:

- `user not found: {username}` - User doesn't exist in database
- `user {username} has no roles assigned, defaulting to ROLE_USER` - User has no roles

### Common Issues:

1. **"Loading..." shows forever**
    - Check browser console for errors
    - Verify `/api/current-user` endpoint is accessible
    - Check authentication is working

2. **Non-admin can still see RunAppUsers**
    - Check database - user might have ROLE_ADMIN
    - Clear browser cache and reload
    - Check `/api/current-user` response in Network tab

3. **Admin can't access RunAppUsers**
    - Check database - user might not have ROLE_ADMIN assigned
    - Verify role name is exactly "ROLE_ADMIN" (case-sensitive)
    - Check backend logs for role loading

4. **Regular user can't access any pages**
    - Check if user has at least ROLE_USER in database
    - If no roles, should auto-assign ROLE_USER (check logs)
    - Verify other endpoints allow ROLE_USER (e.g., GarminRuns)

## Authorization Matrix

| Resource           | Endpoint                               | ROLE_ADMIN | ROLE_USER |
|--------------------|----------------------------------------|------------|-----------|
| Current User       | GET /api/current-user                  | ✅          | ✅         |
| Run App Users      | GET/POST/PUT/DELETE /api/runAppUsers/* | ✅          | ❌         |
| Runner App Roles   | GET /api/runnerAppRoles                | ✅          | ✅         |
| Runner App Roles   | POST/PUT/DELETE /api/runnerAppRoles/*  | ✅          | ❌         |
| Garmin Runs        | All /api/garminRuns/*                  | ✅          | ✅         |
| Strava Runs        | All /api/stravaRuns/*                  | ✅          | ✅         |
| Shedlocks          | All /api/shedlocks/*                   | ✅          | ✅         |
| File Name Trackers | All /api/fileNameTrackers/*            | ✅          | ✅         |

## Quick Fix Commands

If you need to quickly assign admin role to a user:

```sql
-- Make user admin@runsapp.com an admin
INSERT INTO run_app_user_roles (user_id, role_id)
SELECT u.id, r.id
FROM run_app_user u,
     runner_app_role r
WHERE u.email = 'admin@runsapp.com'
  AND r.role_name = 'ROLE_ADMIN'
  AND NOT EXISTS (SELECT 1
                  FROM run_app_user_roles ur2
                  WHERE ur2.user_id = u.id
                    AND ur2.role_id = r.id);
```

```sql
-- Make runner@runsapp.com a regular user
INSERT INTO run_app_user_roles (user_id, role_id)
SELECT u.id, r.id
FROM run_app_user u,
     runner_app_role r
WHERE u.email = 'runner@runsapp.com'
  AND r.role_name = 'ROLE_USER'
  AND NOT EXISTS (SELECT 1
                  FROM run_app_user_roles ur2
                  WHERE ur2.user_id = u.id
                    AND ur2.role_id = r.id);
```
