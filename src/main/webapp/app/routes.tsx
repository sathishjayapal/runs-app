import React from 'react';
import { createBrowserRouter, RouterProvider } from 'react-router';
import App from "./app";
import Home from './home/home';
import RunAppUserList from './run-app-user/run-app-user-list';
import RunAppUserAdd from './run-app-user/run-app-user-add';
import RunAppUserEdit from './run-app-user/run-app-user-edit';
import GarminRunList from './garmin-run/garmin-run-list';
import GarminRunAdd from './garmin-run/garmin-run-add';
import GarminRunEdit from './garmin-run/garmin-run-edit';
import ShedlockList from './shedlock/shedlock-list';
import ShedlockAdd from './shedlock/shedlock-add';
import ShedlockEdit from './shedlock/shedlock-edit';
import FileNameTrackerList from './file-name-tracker/file-name-tracker-list';
import FileNameTrackerAdd from './file-name-tracker/file-name-tracker-add';
import FileNameTrackerEdit from './file-name-tracker/file-name-tracker-edit';
import StravaRunList from './strava-run/strava-run-list';
import StravaRunAdd from './strava-run/strava-run-add';
import StravaRunEdit from './strava-run/strava-run-edit';
import Error from './error/error';


export default function AppRoutes() {
  const router = createBrowserRouter([
    {
      element: <App />,
      children: [
        { path: '', element: <Home /> },
        { path: 'runAppUsers', element: <RunAppUserList /> },
        { path: 'runAppUsers/add', element: <RunAppUserAdd /> },
        { path: 'runAppUsers/edit/:id', element: <RunAppUserEdit /> },
        { path: 'garminRuns', element: <GarminRunList /> },
        { path: 'garminRuns/add', element: <GarminRunAdd /> },
        { path: 'garminRuns/edit/:id', element: <GarminRunEdit /> },
        { path: 'shedlocks', element: <ShedlockList /> },
        { path: 'shedlocks/add', element: <ShedlockAdd /> },
        { path: 'shedlocks/edit/:name', element: <ShedlockEdit /> },
        { path: 'fileNameTrackers', element: <FileNameTrackerList /> },
        { path: 'fileNameTrackers/add', element: <FileNameTrackerAdd /> },
        { path: 'fileNameTrackers/edit/:id', element: <FileNameTrackerEdit /> },
        { path: 'stravaRuns', element: <StravaRunList /> },
        { path: 'stravaRuns/add', element: <StravaRunAdd /> },
        { path: 'stravaRuns/edit/:runNumber', element: <StravaRunEdit /> },
        { path: 'error', element: <Error /> },
        { path: '*', element: <Error /> }
      ]
    }
  ]);

  return (
    <RouterProvider router={router} />
  );
}
