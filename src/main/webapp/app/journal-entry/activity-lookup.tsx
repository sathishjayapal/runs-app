import React, { useEffect, useState } from 'react';
import { useTranslation } from 'react-i18next';
import axios from 'axios';
import { GarminRunDTO } from 'app/garmin-run/garmin-run-model';

interface ActivityLookupProps {
  value: string | null | undefined;
  onChange: (activityId: string) => void;
  error?: string;
}

export default function ActivityLookup({ value, onChange, error }: ActivityLookupProps) {
  const { t } = useTranslation();
  const [activities, setActivities] = useState<GarminRunDTO[]>([]);
  const [filteredActivities, setFilteredActivities] = useState<GarminRunDTO[]>([]);
  const [searchQuery, setSearchQuery] = useState('');
  const [showDropdown, setShowDropdown] = useState(false);
  const [loading, setLoading] = useState(true);
  const [selectedActivity, setSelectedActivity] = useState<GarminRunDTO | null>(null);

  const fetchRecentActivities = async () => {
    try {
      const response = await axios.get('/api/garminRuns?page=0&size=50&sort=activityDate,DESC');
      const runs = response.data.content || [];
      setActivities(runs);
      setFilteredActivities(runs);

      // If value is provided, find and set the selected activity
      if (value) {
        const found = runs.find((r: GarminRunDTO) => r.activityId === value);
        if (found) {
          setSelectedActivity(found);
        }
      }
    } catch (error: any) {
      console.error('Failed to fetch activities:', error);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchRecentActivities();
  }, []);

  const handleSearch = (query: string) => {
    setSearchQuery(query);
    if (!query.trim()) {
      setFilteredActivities(activities);
      return;
    }

    const lowerQuery = query.toLowerCase();
    const filtered = activities.filter((activity: GarminRunDTO) => {
      const name = (activity.activityName || '').toLowerCase();
      const date = activity.activityDate ? new Date(activity.activityDate).toLocaleDateString() : '';
      const id = (activity.activityId || '').toLowerCase();
      return name.includes(lowerQuery) || date.includes(lowerQuery) || id.includes(lowerQuery);
    });
    setFilteredActivities(filtered);
  };

  const handleSelectActivity = (activity: GarminRunDTO) => {
    if (activity.activityId) {
      setSelectedActivity(activity);
      onChange(activity.activityId);
      setShowDropdown(false);
      setSearchQuery('');
    }
  };

  const handleClear = () => {
    setSelectedActivity(null);
    onChange('');
    setSearchQuery('');
    setFilteredActivities(activities);
  };

  return (
    <div className="mb-6">
      <label className="block text-sm font-semibold text-gray-800 mb-3">
        {t('journalEntry.activityId.label')}
      </label>

      {selectedActivity && (
        <div className="mb-4 p-4 bg-gradient-to-r from-blue-50 to-indigo-50 border-2 border-blue-200 rounded-lg shadow-sm hover:shadow-md transition">
          <div className="flex justify-between items-start gap-4">
            <div className="flex-1">
              <div className="flex items-center gap-2 mb-2">
                <svg className="w-5 h-5 text-blue-600" fill="currentColor" viewBox="0 0 20 20">
                  <path d="M5.5 13a3.5 3.5 0 01-.369-6.98 4 4 0 117.753-1.3A4.5 4.5 0 1113.5 13H11V9.413l1.293 1.293a1 1 0 001.414-1.414l-3-3a1 1 0 00-1.414 0l-3 3a1 1 0 001.414 1.414L9 9.414V13H5.5z" />
                </svg>
                <p className="font-bold text-gray-900 text-lg">{selectedActivity.activityName || 'Unnamed Activity'}</p>
              </div>
              <div className="grid grid-cols-3 gap-4 mt-3 mb-2">
                <div>
                  <p className="text-xs font-semibold text-gray-600 uppercase">Date</p>
                  <p className="text-sm text-gray-800 font-medium">
                    {selectedActivity.activityDate
                      ? new Date(selectedActivity.activityDate).toLocaleDateString()
                      : 'No date'}
                  </p>
                </div>
                <div>
                  <p className="text-xs font-semibold text-gray-600 uppercase">Type</p>
                  <p className="text-sm text-gray-800 font-medium">{selectedActivity.activityType || '-'}</p>
                </div>
                <div>
                  <p className="text-xs font-semibold text-gray-600 uppercase">Distance</p>
                  <p className="text-sm text-gray-800 font-medium">{selectedActivity.distance || '-'} km</p>
                </div>
              </div>
              <p className="text-xs text-gray-500 mt-2 font-mono">ID: {selectedActivity.activityId}</p>
            </div>
            <button
              type="button"
              onClick={handleClear}
              className="px-3 py-2 text-blue-600 hover:text-blue-800 hover:bg-blue-100 rounded font-semibold text-sm transition whitespace-nowrap"
            >
              ✕ Change
            </button>
          </div>
        </div>
      )}

      {!selectedActivity && (
        <div className="relative">
          <div className="relative">
            <svg className="absolute left-3 top-3 w-5 h-5 text-gray-400" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M21 21l-6-6m2-5a7 7 0 11-14 0 7 7 0 0114 0z" />
            </svg>
            <input
              type="text"
              placeholder={t('journalEntry.activityLookup.searchPlaceholder') || 'Search activities...'}
              value={searchQuery}
              onChange={(e) => handleSearch(e.target.value)}
              onFocus={() => setShowDropdown(true)}
              className={`w-full pl-10 pr-4 py-3 border-2 rounded-lg focus:outline-none focus:ring-2 focus:ring-blue-500 focus:border-transparent transition ${
                error ? 'border-red-500' : 'border-gray-300 hover:border-gray-400'
              }`}
              disabled={loading}
            />
          </div>

          {showDropdown && !loading && (
            <div className="absolute z-10 w-full mt-2 bg-white border-2 border-gray-200 rounded-lg shadow-xl max-h-96 overflow-y-auto">
              {filteredActivities.length === 0 ? (
                <div className="p-8 text-center">
                  <svg className="w-12 h-12 text-gray-300 mx-auto mb-3" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                    <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={1.5} d="M20 21l-4.35-4.35m0 0a7 7 0 10-9.9 0l4.35 4.35m5.55-5.55a7 7 0 11-9.9 0" />
                  </svg>
                  <p className="text-gray-500 text-sm">
                    {activities.length === 0
                      ? t('journalEntry.activityLookup.noActivities') || 'No activities found'
                      : t('journalEntry.activityLookup.noMatches') || 'No matches found'}
                  </p>
                </div>
              ) : (
                <div className="py-2">
                  {filteredActivities.map((activity: GarminRunDTO) => (
                    <button
                      key={activity.id}
                      type="button"
                      onClick={() => handleSelectActivity(activity)}
                      className="w-full text-left px-4 py-3 hover:bg-blue-50 transition border-b border-gray-100 last:border-b-0"
                    >
                      <div className="flex items-start justify-between">
                        <div className="flex-1">
                          <p className="font-semibold text-gray-900">{activity.activityName || 'Unnamed'}</p>
                          <p className="text-sm text-gray-600 mt-1">
                            <span className="inline-block">
                              📅 {activity.activityDate
                                ? new Date(activity.activityDate).toLocaleDateString()
                                : 'No date'}
                            </span>
                            {' '} · {' '}
                            <span className="inline-block">🏃 {activity.activityType}</span>
                            {' '} · {' '}
                            <span className="inline-block">📏 {activity.distance} km</span>
                          </p>
                          <p className="text-xs text-gray-400 mt-2 font-mono">ID: {activity.activityId}</p>
                        </div>
                        <svg className="w-5 h-5 text-gray-300 mt-1 flex-shrink-0" fill="currentColor" viewBox="0 0 20 20">
                          <path fillRule="evenodd" d="M7.293 14.707a1 1 0 010-1.414L10.586 10 7.293 6.707a1 1 0 011.414-1.414l4 4a1 1 0 010 1.414l-4 4a1 1 0 01-1.414 0z" clipRule="evenodd" />
                        </svg>
                      </div>
                    </button>
                  ))}
                </div>
              )}
            </div>
          )}
        </div>
      )}

      {error && (
        <div className="mt-2 p-2 bg-red-50 border border-red-200 rounded">
          <p className="text-red-700 text-sm font-medium">{error}</p>
        </div>
      )}
    </div>
  );
}
