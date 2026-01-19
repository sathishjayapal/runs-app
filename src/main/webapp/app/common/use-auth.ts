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
            } catch (error) {
                console.error('Failed to fetch current user:', error);
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

    const logout = (): void => {
        // Navigate to logout page which handles the logout process
        window.location.href = '/user-logout';
    };

    return {
        currentUser,
        loading,
        hasRole,
        isAdmin,
        logout,
    };
}
