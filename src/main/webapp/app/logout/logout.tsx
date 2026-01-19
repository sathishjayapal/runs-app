import React, {useState} from 'react';


export default function Logout() {
    const [isLoggingOut, setIsLoggingOut] = useState(false);

    const handleLogout = () => {
        setIsLoggingOut(true);

        // Use XMLHttpRequest to send invalid credentials
        // This is the most reliable way to clear HTTP Basic Auth in browsers
        const xhr = new XMLHttpRequest();
        xhr.open('GET', '/api/current-user', true, 'logout', 'logout');

        xhr.onreadystatechange = function () {
            if (xhr.readyState === 4) {
                // After the invalid auth attempt, force a full page reload with timestamp
                // This ensures the browser prompts for credentials and React re-fetches user data
                setTimeout(() => {
                    window.location.href = '/?t=' + Date.now();
                }, 500);
            }
        };

        xhr.send();
    };

    const handleReturnHome = () => {
        window.location.href = '/';
    };

    return (
        <div className="flex items-center justify-center min-h-screen bg-gray-100">
            <div className="bg-white p-8 rounded-lg shadow-md max-w-md w-full text-center">
                {isLoggingOut ? (
                    <>
                        <div
                            className="animate-spin rounded-full h-12 w-12 border-b-2 border-blue-600 mx-auto mb-4"></div>
                        <h2 className="text-2xl font-semibold mb-2">Logging out...</h2>
                        <p className="text-gray-600">Clearing credentials and redirecting...</p>
                    </>
                ) : (
                    <>
                        <svg className="w-16 h-16 text-blue-500 mx-auto mb-4" fill="none" stroke="currentColor"
                             viewBox="0 0 24 24">
                            <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2}
                                  d="M17 16l4-4m0 0l-4-4m4 4H7m6 4v1a3 3 0 01-3 3H6a3 3 0 01-3-3V7a3 3 0 013-3h4a3 3 0 013 3v1"/>
                        </svg>
                        <h2 className="text-2xl font-semibold mb-4">Logout</h2>
                        <p className="text-gray-600 mb-6">Click the button below to log out and switch to a different
                            user account.</p>

                        <div className="space-y-3">
                            <button
                                onClick={handleLogout}
                                className="w-full bg-blue-600 hover:bg-blue-700 text-white font-medium py-2 px-6 rounded"
                            >
                                Logout & Switch User
                            </button>
                            <button
                                onClick={handleReturnHome}
                                className="w-full bg-gray-200 hover:bg-gray-300 text-gray-700 font-medium py-2 px-6 rounded"
                            >
                                Cancel
                            </button>
                        </div>

                        <div className="mt-6 pt-6 border-t border-gray-200">
                            <p className="text-sm text-gray-500">
                                <strong>How it works:</strong> Clicking logout will clear your current session and
                                prompt you to log in again.
                                Enter different credentials to switch users.
                            </p>
                            <p className="text-xs text-gray-400 mt-2">
                                Available users: admin@runsapp.com / runner@runsapp.com
                            </p>
                        </div>
                    </>
                )}
            </div>
        </div>
    );
}
