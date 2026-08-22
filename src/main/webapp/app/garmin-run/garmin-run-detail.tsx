import React, { useEffect, useState } from 'react';
import { useTranslation } from 'react-i18next';
import { Link, useNavigate, useParams } from 'react-router';
import { handleServerError } from 'app/common/utils';
import { GarminRunDTO } from 'app/garmin-run/garmin-run-model';
import { JournalEntryDTO } from 'app/journal-entry/journal-entry-model';
import axios from 'axios';
import useDocumentTitle from 'app/common/use-document-title';

export default function GarminRunDetail() {
  const { t } = useTranslation();
  const { id } = useParams<{ id: string }>();
  const navigate = useNavigate();

  const [garminRun, setGarminRun] = useState<GarminRunDTO | undefined>(undefined);
  const [journalEntries, setJournalEntries] = useState<JournalEntryDTO[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  useDocumentTitle(t('garminRun.detail.headline'));

  const feelBadgeClass = (feel: string | null | undefined) => {
    switch (feel) {
      case 'GREAT':
        return 'bg-green-100 text-green-800';
      case 'GOOD':
        return 'bg-blue-100 text-blue-800';
      case 'OK':
        return 'bg-gray-100 text-gray-800';
      case 'ROUGH':
        return 'bg-yellow-100 text-yellow-800';
      case 'BAD':
        return 'bg-red-100 text-red-800';
      default:
        return 'bg-gray-100 text-gray-500';
    }
  };

  const getRunDetail = async () => {
    try {
      const runResponse = await axios.get(`/api/garminRuns/${id}`);
      setGarminRun(runResponse.data);

      // Fetch journal entries for this activity
      if (runResponse.data.activityId) {
        try {
          const entriesResponse = await axios.get(
            `/api/journal/activity/${runResponse.data.activityId}`
          );
          setJournalEntries(Array.isArray(entriesResponse.data) ? entriesResponse.data : []);
        } catch (err: any) {
          // Journal entries endpoint might not exist or no entries found
          console.log('Could not fetch journal entries:', err.message);
          setJournalEntries([]);
        }
      }
    } catch (error: any) {
      if (error?.response?.status === 401) {
        window.location.href = '/login';
        return;
      }
      handleServerError(error, navigate);
      setError(t('garminRun.detail.error'));
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    getRunDetail();
  }, [id]);

  if (loading) {
    return <div className="text-center py-4">{t('common.loading')}</div>;
  }

  if (!garminRun) {
    return (
      <div>
        <p className="text-red-600">{error || t('garminRun.detail.notFound')}</p>
        <Link to="/garminRuns" className="text-blue-600 hover:underline mt-4 inline-block">
          {t('garminRun.detail.backToList')}
        </Link>
      </div>
    );
  }

  return (
    <>
      <div className="mb-6">
        <Link to="/garminRuns" className="text-blue-600 hover:underline text-sm mb-4 inline-block">
          ← {t('garminRun.detail.backToList')}
        </Link>
        <div className="flex flex-wrap justify-between items-center">
          <h1 className="text-3xl md:text-4xl font-medium">
            {garminRun.activityName || garminRun.activityType}
          </h1>
          <div className="flex gap-2">
            <Link
              to={'/garminRuns/edit/' + garminRun.id}
              className="inline-flex items-center text-white bg-blue-600 hover:bg-blue-700 focus:ring-4 focus:ring-blue-300 rounded px-4 py-2"
            >
              <svg className="w-4 h-4 mr-2" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M11 5H6a2 2 0 00-2 2v11a2 2 0 002 2h11a2 2 0 002-2v-5m-1.414-9.414a2 2 0 112.828 2.828L11.828 15H9v-2.828l8.586-8.586z" />
              </svg>
              {t('garminRun.detail.edit')}
            </Link>
          </div>
        </div>
      </div>

      {/* Run Details Card */}
      <div className="bg-white border border-gray-200 rounded-lg shadow-md p-6 mb-6">
        <h2 className="text-xl font-semibold mb-4">{t('garminRun.detail.runInfo')}</h2>
        <div className="grid grid-cols-1 md:grid-cols-2 gap-6">
          <div>
            <p className="text-gray-600 text-sm font-medium">{t('garminRun.activityId.label')}</p>
            <p className="text-lg font-mono">{garminRun.activityId || '-'}</p>
          </div>
          <div>
            <p className="text-gray-600 text-sm font-medium">{t('garminRun.activityType.label')}</p>
            <p className="text-lg">{garminRun.activityType || '-'}</p>
          </div>
          <div>
            <p className="text-gray-600 text-sm font-medium">{t('garminRun.activityDate.label')}</p>
            <p className="text-lg">
              {garminRun.activityDate
                ? new Date(garminRun.activityDate).toLocaleDateString()
                : '-'}
            </p>
          </div>
          <div>
            <p className="text-gray-600 text-sm font-medium">{t('garminRun.distance.label')}</p>
            <p className="text-lg font-semibold text-blue-600">{garminRun.distance || '-'}</p>
          </div>
          <div>
            <p className="text-gray-600 text-sm font-medium">{t('garminRun.elapsedTime.label')}</p>
            <p className="text-lg">{garminRun.elapsedTime || '-'}</p>
          </div>
          <div>
            <p className="text-gray-600 text-sm font-medium">{t('garminRun.maxHeartRate.label')}</p>
            <p className="text-lg">{garminRun.maxHeartRate || '-'}</p>
          </div>
          <div>
            <p className="text-gray-600 text-sm font-medium">{t('garminRun.calories.label')}</p>
            <p className="text-lg font-semibold text-orange-600">{garminRun.calories || '-'}</p>
          </div>
          <div>
            <p className="text-gray-600 text-sm font-medium">{t('garminRun.createdBy.label')}</p>
            <p className="text-lg">{garminRun.createdByName || '-'}</p>
          </div>
        </div>
        {garminRun.activityDescription && (
          <div className="mt-6 pt-6 border-t border-gray-200">
            <p className="text-gray-600 text-sm font-medium mb-2">
              {t('garminRun.activityDescription.label')}
            </p>
            <p className="text-gray-800">{garminRun.activityDescription}</p>
          </div>
        )}
      </div>

      {/* Linked Journal Entries Section */}
      <div className="bg-white border border-gray-200 rounded-lg shadow-md p-6">
        <div className="flex justify-between items-center mb-4">
          <h2 className="text-xl font-semibold">{t('garminRun.detail.linkedJournalEntries')}</h2>
          <Link
            to={`/journalEntries/add?activityId=${garminRun.activityId}`}
            className="inline-flex items-center text-white bg-green-600 hover:bg-green-700 focus:ring-4 focus:ring-green-300 rounded px-4 py-2 text-sm"
          >
            <svg className="w-4 h-4 mr-2" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M12 4v16m8-8H4" />
            </svg>
            {t('garminRun.detail.addJournalEntry')}
          </Link>
        </div>

        {journalEntries.length === 0 ? (
          <div className="text-gray-500 text-center py-8">
            {t('garminRun.detail.noJournalEntries')}
          </div>
        ) : (
          <div className="space-y-4">
            {journalEntries.map((entry) => (
              <div
                key={entry.id}
                className="border border-gray-300 rounded-lg p-4 hover:bg-gray-50 transition"
              >
                <div className="flex justify-between items-start mb-3">
                  <div className="flex items-center gap-3">
                    <h3 className="font-semibold text-gray-800">
                      {entry.entryDate
                        ? new Date(entry.entryDate).toLocaleDateString()
                        : 'No date'}
                    </h3>
                    {entry.feel && (
                      <span
                        className={
                          'text-xs font-semibold px-2 py-1 rounded-full ' +
                          feelBadgeClass(entry.feel)
                        }
                      >
                        {entry.feel}
                      </span>
                    )}
                    {entry.embedded && (
                      <span className="text-xs font-semibold px-2 py-1 rounded-full bg-purple-100 text-purple-800">
                        {t('garminRun.detail.aiIndexed')}
                      </span>
                    )}
                    {!entry.embedded && entry.id && (
                      <span className="text-xs font-semibold px-2 py-1 rounded-full bg-gray-100 text-gray-800">
                        {t('garminRun.detail.pending')}
                      </span>
                    )}
                  </div>
                  <Link
                    to={'/journalEntries/edit/' + entry.id}
                    className="text-blue-600 hover:underline text-sm"
                  >
                    {t('garminRun.detail.viewEntry')}
                  </Link>
                </div>

                {entry.perceivedEffort && (
                  <p className="text-sm text-gray-600 mb-2">
                    <span className="font-medium">{t('journalEntry.perceivedEffort.label')}:</span>{' '}
                    {entry.perceivedEffort}/10
                  </p>
                )}

                {entry.bodyNotes && (
                  <div className="mb-2">
                    <p className="text-sm text-gray-600">
                      <span className="font-medium">{t('journalEntry.bodyNotes.label')}:</span>
                    </p>
                    <p className="text-sm text-gray-800 ml-2">{entry.bodyNotes}</p>
                  </div>
                )}

                {entry.contextNotes && (
                  <div className="mb-2">
                    <p className="text-sm text-gray-600">
                      <span className="font-medium">{t('journalEntry.contextNotes.label')}:</span>
                    </p>
                    <p className="text-sm text-gray-800 ml-2">{entry.contextNotes}</p>
                  </div>
                )}

                {entry.narrative && (
                  <div>
                    <p className="text-sm text-gray-600">
                      <span className="font-medium">{t('journalEntry.narrative.label')}:</span>
                    </p>
                    <p className="text-sm text-gray-800 ml-2 line-clamp-3">{entry.narrative}</p>
                  </div>
                )}
              </div>
            ))}
          </div>
        )}
      </div>
    </>
  );
}
