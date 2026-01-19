import React from 'react';
import {Navigate} from 'react-router';
import {useAuth} from 'app/common/use-auth';

interface AdminRouteProps {
    children: React.ReactNode;
}

export default function AdminRoute({children}: AdminRouteProps) {
    const {isAdmin, loading} = useAuth();

    if (loading) {
        return <div className="text-center p-8">Loading...</div>;
    }

    if (!isAdmin()) {
        return <Navigate to="/" replace/>;
    }

    return <>{children}</>;
}
