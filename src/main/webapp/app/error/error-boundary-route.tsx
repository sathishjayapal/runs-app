import React from 'react';
import { useRouteError, isRouteErrorResponse } from 'react-router';

export default function ErrorBoundaryRoute() {
  const error = useRouteError();

  let errorMessage: string;
  let errorStatus: string;

  if (isRouteErrorResponse(error)) {
    errorStatus = error.status.toString();
    errorMessage = error.statusText || error.data?.message || 'An error occurred';
  } else if (error instanceof Error) {
    errorStatus = '500';
    errorMessage = error.message;
  } else {
    errorStatus = '500';
    errorMessage = 'Unknown error occurred';
  }

  console.error('Route error:', error);

  return (
    <div className="container mx-auto px-4 md:px-6 my-12">
      <h1 className="text-3xl md:text-4xl font-medium mb-8">
        {errorStatus} - Error
      </h1>
      <div className="bg-red-200 border-red-800 text-red-800 border rounded p-4 mb-6">
        <p className="font-semibold mb-2">Something went wrong:</p>
        <p>{errorMessage}</p>
      </div>
      <a href="/" className="text-blue-600 hover:underline">
        ← Back to Home
      </a>
    </div>
  );
}
