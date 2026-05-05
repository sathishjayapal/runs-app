import React from 'react';
import {createBrowserRouter, RouterProvider} from 'react-router';
import App from "./app";
import Home from './home/home';
import RunAppUserList from './run-app-user/run-app-user-list';
import RunAppUserAdd from './run-app-user/run-app-user-add';
import RunAppUserEdit from './run-app-user/run-app-user-edit';
import GarminRunList from './garmin-run/garmin-run-list';
import GarminRunAdd from './garmin-run/garmin-run-add';
import GarminRunEdit from './garmin-run/garmin-run-edit';
import ShedlockList from './shedlock/shedlock-list';
import StravaRunList from './strava-run/strava-run-list';
import StravaRunAdd from './strava-run/strava-run-add';
import StravaRunEdit from './strava-run/strava-run-edit';
import FileImportRecordList from './file-import-record/file-import-record-list';
import FileImportRecordDetail from './file-import-record/file-import-record-detail';
import Error from './error/error';
import ErrorBoundaryRoute from './error/error-boundary-route';
import AdminRoute from './common/admin-route';
import Logout from './logout/logout';
import Login from './login/login';


export default function AppRoutes() {
  const router = createBrowserRouter([
    {
      element: <App />,
      errorElement: <ErrorBoundaryRoute />,
      children: [
        { path: '', element: <Home /> },
        {path: 'runAppUsers', element: <AdminRoute><RunAppUserList/></AdminRoute>},
        {path: 'runAppUsers/add', element: <AdminRoute><RunAppUserAdd/></AdminRoute>},
        {path: 'runAppUsers/edit/:id', element: <AdminRoute><RunAppUserEdit/></AdminRoute>},
        { path: 'garminRuns', element: <GarminRunList /> },
        { path: 'garminRuns/add', element: <GarminRunAdd /> },
        { path: 'garminRuns/edit/:id', element: <GarminRunEdit /> },
        {path: 'shedlocks', element: <AdminRoute><ShedlockList/></AdminRoute>},
        { path: 'fileImportRecords', element: <FileImportRecordList /> },
        { path: 'fileImportRecords/:id', element: <FileImportRecordDetail /> },
        {path: 'user-logout', element: <Logout/>},
        {path: 'login', element: <Login/>},
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
