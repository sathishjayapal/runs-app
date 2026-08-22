import React, { useEffect, useState } from 'react';
import { useTranslation } from 'react-i18next';
import { Link, useNavigate, useSearchParams } from 'react-router';
import { handleServerError, getListParams } from 'app/common/utils';
import { GarminRunDTO } from 'app/garmin-run/garmin-run-model';
import { PagedModel, Pagination } from 'app/common/list-helper/pagination';
import axios from 'axios';
import SearchFilter from 'app/common/list-helper/search-filter';
import Sorting from 'app/common/list-helper/sorting';
import useDocumentTitle from 'app/common/use-document-title';


export default function GarminRunList() {
  const { t } = useTranslation();
  useDocumentTitle(t('garminRun.list.headline'));

  const [garminRuns, setGarminRuns] = useState<PagedModel<GarminRunDTO>|undefined>(undefined);
  const navigate = useNavigate();
  const [searchParams, ] = useSearchParams();
  const listParams = getListParams();
  const sortOptions = {
    'id,ASC': t('garminRun.list.sort.id,ASC'),
    'activityId,ASC': t('garminRun.list.sort.activityId,ASC'),
    'activityDate,ASC': t('garminRun.list.sort.activityDate,ASC'),
    'activityDate,DESC': t('garminRun.list.sort.activityDate,DESC')
  };
  const totalElements = garminRuns?.page?.totalElements ?? 0;
  const content = garminRuns?.content ?? [];

  const getAllGarminRuns = async () => {
    try {
      const response = await axios.get('/api/garminRuns?' + listParams);
      setGarminRuns(response.data);
    } catch (error: any) {
      if (error?.response?.status === 401) {
        window.location.href = '/login';
        return;
      }
      handleServerError(error, navigate);
    }
  };

  const confirmDelete = async (id: number) => {
    if (!confirm(t('delete.confirm'))) {
      return;
    }
    try {
      await axios.delete('/api/garminRuns/' + id);
      navigate('/garminRuns', {
            state: {
              msgInfo: t('garminRun.delete.success')
            }
          });
      getAllGarminRuns();
    } catch (error: any) {
      handleServerError(error, navigate);
    }
  };

  useEffect(() => {
    getAllGarminRuns();
  }, [searchParams]);

  return (<>
    <div className="flex flex-wrap mb-6">
      <h1 className="grow text-3xl md:text-4xl font-medium mb-2">{t('garminRun.list.headline')}</h1>
      <div>
        <Link to="/garminRuns/add" className="inline-block text-white bg-blue-600 hover:bg-blue-700 focus:ring-blue-300  focus:ring-4 rounded px-5 py-2">{t('garminRun.list.createNew')}</Link>
      </div>
    </div>
    {( (totalElements !== 0 && garminRuns?.page) || searchParams.get('filter')) && (
    <div className="flex flex-wrap justify-between">
      <SearchFilter placeholder={t('garminRun.list.filter')} />
      <Sorting sortOptions={sortOptions} />
    </div>
    )}
    {!garminRuns || totalElements === 0 ? (
    <div>{t('garminRun.list.empty')}</div>
    ) : (<>
    <div className="overflow-x-auto">
      <table className="w-full border-collapse">
        <thead>
          <tr className="bg-gray-100">
            <th className="border border-gray-300 px-4 py-2 text-left">{t('garminRun.activityId.label')}</th>
            <th className="border border-gray-300 px-4 py-2 text-left">{t('garminRun.activityName.label')}</th>
            <th className="border border-gray-300 px-4 py-2 text-center">{t('garminRun.activityDate.label')}</th>
            <th className="border border-gray-300 px-4 py-2 text-center">{t('garminRun.distance.label')}</th>
            <th className="border border-gray-300 px-4 py-2 text-center">{t('garminRun.calories.label')}</th>
            <th className="border border-gray-300 px-4 py-2 text-left">{t('garminRun.createdBy.label')}</th>
            <th className="border border-gray-300 px-4 py-2 text-center">{t('garminRun.list.actions')}</th>
          </tr>
        </thead>
        <tbody>
          {content.map((garminRun) => (
          <tr key={garminRun.id} className="hover:bg-gray-50">
            <td className="border border-gray-300 px-4 py-2 font-mono text-sm">{garminRun.activityId}</td>
            <td className="border border-gray-300 px-4 py-2">
              <Link to={'/garminRuns/' + garminRun.id} className="text-blue-600 hover:underline font-medium">
                {garminRun.activityName}
              </Link>
            </td>
            <td className="border border-gray-300 px-4 py-2 text-center text-sm">
              {garminRun.activityDate ? new Date(garminRun.activityDate).toLocaleDateString() : '-'}
            </td>
            <td className="border border-gray-300 px-4 py-2 text-center font-semibold text-blue-600">{garminRun.distance}</td>
            <td className="border border-gray-300 px-4 py-2 text-center font-semibold text-orange-600">{garminRun.calories || '-'}</td>
            <td className="border border-gray-300 px-4 py-2">{garminRun.createdByName}</td>
            <td className="border border-gray-300 px-4 py-2 text-center">
              <div className="flex gap-2 justify-center">
                <Link to={'/garminRuns/edit/' + garminRun.id} className="inline-flex items-center text-white bg-blue-600 hover:bg-blue-700 focus:ring-4 focus:ring-blue-300 rounded px-3 py-1.5 text-sm font-medium transition-colors">
                  <svg className="w-4 h-4 mr-1" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                    <path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M11 5H6a2 2 0 00-2 2v11a2 2 0 002 2h11a2 2 0 002-2v-5m-1.414-9.414a2 2 0 112.828 2.828L11.828 15H9v-2.828l8.586-8.586z" />
                  </svg>
                  {t('garminRun.list.edit')}
                </Link>
                <button type="button" onClick={() => confirmDelete(garminRun.id!)} className="inline-flex items-center text-white bg-red-600 hover:bg-red-700 focus:ring-4 focus:ring-red-300 rounded px-3 py-1.5 text-sm font-medium cursor-pointer transition-colors">
                  <svg className="w-4 h-4 mr-1" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                    <path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M19 7l-.867 12.142A2 2 0 0116.138 21H7.862a2 2 0 01-1.995-1.858L5 7m5 4v6m4-6v6m1-10V4a1 1 0 00-1-1h-4a1 1 0 00-1 1v3M4 7h16" />
                  </svg>
                  {t('garminRun.list.delete')}
                </button>
              </div>
            </td>
          </tr>
          ))}
        </tbody>
      </table>
    </div>
    {garminRuns?.page && <Pagination page={garminRuns.page} />}
    </>)}
  </>);
}
