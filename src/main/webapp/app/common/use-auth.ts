import {useEffect, useState} from 'react';
import axios from 'axios';

interface CurrentUser {
    username: string;
    roles: string[];
}

export function useAuth() {
    const [currentUser, setCurrentUser] = useState<CurrentUser | null>(null);
    const [loading, setLoading] = useState(true);

    useEffect(() => {
        const fetchCurrentUser = async () => {
            try {
                const response = await axios.get('/api/current-user');
                setCurrentUser(response.data);
            } catch (error: any) {
                if (error.response?.status === 401) {
                    console.log('User not authenticated');
                } else {
                    console.error('Failed to fetch current user:', error);
                }
                setCurrentUser(null);
            } finally {
                setLoading(false);
            }
        };

        fetchCurrentUser();
    }, []);

    const hasRole = (role: string): boolean => {
        return currentUser?.roles?.includes(role) ?? false;
    };

    const isAdmin = (): boolean => {
        return hasRole('ROLE_ADMIN');
    };

    const logout = async (): Promise<void> => {
        try {
            // Call Spring Security logout endpoint to properly clear session
            await axios.post('/logout');
        } catch (error) {
            // Logout may fail if session already expired, but we still proceed
            console.log('Logout request completed');
        } finally {
            // Force a complete page reload to clear all cached state and fetch fresh data
            // This ensures the useAuth hook runs again and detects the cleared session
            window.location.href = '/login?t=' + Date.now();
        }
    };

    return {
        currentUser,
        loading,
        hasRole,
        isAdmin,
        logout,
    };
}
