import React from 'react';
import {Link} from 'react-router';
import {useTranslation} from 'react-i18next';
import useDocumentTitle from 'app/common/use-document-title';
import {useAuth} from 'app/common/use-auth';
import './home.css';


export default function Home() {
  const {t} = useTranslation();
  const {isAdmin} = useAuth();
  useDocumentTitle(t('home.index.headline'));

  return (<>
    <h1 className="grow text-3xl md:text-4xl font-medium mb-8">{t('home.index.headline')}</h1>
    <p className="mb-4">
      <strong>Runs App</strong> is a comprehensive running activity tracker that seamlessly integrates with <strong>Garmin
      devices</strong> and <strong>Strava</strong>.
      Import your running data from .FIT files, sync with Strava, and manage all your activities in one place.
    </p><p className="mb-12">
    <span>Explore the API documentation at</span>
      <span> </span>
    <a href={process.env.API_PATH + '/swagger-ui.html'} target="_blank" className="underline">Swagger UI</a>.
    </p>
    <div className="md:w-2/5 mb-12">
      <h4 className="text-2xl font-medium mb-4">Explore Features</h4>
      <div className="flex flex-col overflow-hidden rounded-lg border border-blue-200 bg-blue-50/40 shadow-sm">
        {isAdmin() &&
            <Link to="/runAppUsers"
                  className="w-full border-b border-blue-100 px-4 py-2 transition-colors hover:bg-blue-100">User
              Management</Link>}
        <Link to="/garminRuns"
              className={`w-full border-b border-blue-100 px-4 py-2 transition-colors hover:bg-blue-100 ${!isAdmin() ? 'rounded-t-lg' : ''}`}>Garmin
          Runs</Link>
        <Link to="/fileImportRecords"
              className="w-full border-b border-blue-100 px-4 py-2 transition-colors hover:bg-blue-100">File
          Import Records</Link>
        <Link to="/stravaRuns" className="w-full px-4 py-2 transition-colors hover:bg-blue-100 rounded-b-lg">Strava
          Runs</Link>
      </div>
    </div>

    <div className="tech-stack-section">
      <h2>Built With</h2>
      <div className="tech-stack-grid">
        <div className="tech-category">
          <h3>Backend</h3>
          <ul>
            <li>Spring Boot + Java</li>
            <li>PostgreSQL Database</li>
            <li>Spring Security + OAuth2</li>
          </ul>
        </div>
        <div className="tech-category">
          <h3>Frontend</h3>
          <ul>
            <li>React + TypeScript</li>
            <li>Webpack</li>
            <li>Tailwind CSS</li>
          </ul>
        </div>
        <div className="tech-category">
          <h3>Integrations</h3>
          <ul>
            <li>Garmin FIT SDK</li>
            <li>Strava API</li>
            <li>Google OAuth</li>
          </ul>
        </div>
      </div>
    </div>
  </>);
}
