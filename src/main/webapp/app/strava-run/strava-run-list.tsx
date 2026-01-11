import React, { useEffect, useState } from 'react';
import { useTranslation } from 'react-i18next';
import { Link, useNavigate, useSearchParams } from 'react-router';
import { handleServerError, getListParams } from 'app/common/utils';
import { StravaRunDTO } from 'app/strava-run/strava-run-model';
import { PagedModel, Pagination } from 'app/common/list-helper/pagination';
import axios from 'axios';
import SearchFilter from 'app/common/list-helper/search-filter';
import Sorting from 'app/common/list-helper/sorting';
import useDocumentTitle from 'app/common/use-document-title';


export default function StravaRunList() {
  const { t } = useTranslation();
  useDocumentTitle(t('stravaRun.list.headline'));

  const [stravaRuns, setStravaRuns] = useState<PagedModel<StravaRunDTO>|undefined>(undefined);
  const navigate = useNavigate();
  const [searchParams, ] = useSearchParams();
  const listParams = getListParams();
  const sortOptions = {
    'runNumber,ASC': t('stravaRun.list.sort.runNumber,ASC'), 
    'customerId,ASC': t('stravaRun.list.sort.customerId,ASC'), 
    'runName,ASC': t('stravaRun.list.sort.runName,ASC')
  };

  const getAllStravaRuns = async () => {
    try {
      const response = await axios.get('/api/stravaRuns?' + listParams);
      setStravaRuns(response.data);
    } catch (error: any) {
      handleServerError(error, navigate);
    }
  };

  const confirmDelete = async (runNumber: number) => {
    if (!confirm(t('delete.confirm'))) {
      return;
    }
    try {
      await axios.delete('/api/stravaRuns/' + runNumber);
      navigate('/stravaRuns', {
            state: {
              msgInfo: t('stravaRun.delete.success')
            }
          });
      getAllStravaRuns();
    } catch (error: any) {
      handleServerError(error, navigate);
    }
  };

  useEffect(() => {
    getAllStravaRuns();
  }, [searchParams]);

  return (<>
    <div className="flex flex-wrap mb-6">
      <h1 className="grow text-3xl md:text-4xl font-medium mb-2">{t('stravaRun.list.headline')}</h1>
      <div>
        <Link to="/stravaRuns/add" className="inline-block text-white bg-blue-600 hover:bg-blue-700 focus:ring-blue-300  focus:ring-4 rounded px-5 py-2">{t('stravaRun.list.createNew')}</Link>
      </div>
    </div>
    {((stravaRuns && stravaRuns.page.totalElements !== 0) || searchParams.get('filter')) && (
    <div className="flex flex-wrap justify-between">
      <SearchFilter placeholder={t('stravaRun.list.filter')} />
      <Sorting sortOptions={sortOptions} />
    </div>
    )}
    {!stravaRuns || stravaRuns.page.totalElements === 0 ? (
    <div>{t('stravaRun.list.empty')}</div>
    ) : (<>
    <div className="overflow-x-auto">
      <table className="w-full">
        <thead>
          <tr>
            <th scope="col" className="text-left p-2">{t('stravaRun.runNumber.label')}</th>
            <th scope="col" className="text-left p-2">{t('stravaRun.customerId.label')}</th>
            <th scope="col" className="text-left p-2">{t('stravaRun.runName.label')}</th>
            <th scope="col" className="text-left p-2">{t('stravaRun.runDate.label')}</th>
            <th scope="col" className="text-left p-2">{t('stravaRun.miles.label')}</th>
            <th scope="col" className="text-left p-2">{t('stravaRun.startLocation.label')}</th>
            <th scope="col" className="text-left p-2">{t('stravaRun.createdBy.label')}</th>
            <th scope="col" className="text-left p-2">{t('stravaRun.updatedBy.label')}</th>
            <th></th>
          </tr>
        </thead>
        <tbody className="border-t-2 border-black">
          {stravaRuns.content.map((stravaRun) => (
          <tr key={stravaRun.runNumber} className="odd:bg-gray-100">
            <td className="p-2">{stravaRun.runNumber}</td>
            <td className="p-2">{stravaRun.customerId}</td>
            <td className="p-2">{stravaRun.runName}</td>
            <td className="p-2">{stravaRun.runDate}</td>
            <td className="p-2">{stravaRun.miles}</td>
            <td className="p-2">{stravaRun.startLocation}</td>
            <td className="p-2">{stravaRun.createdBy}</td>
            <td className="p-2">{stravaRun.updatedBy}</td>
            <td className="p-2">
              <div className="float-right whitespace-nowrap">
                <Link to={'/stravaRuns/edit/' + stravaRun.runNumber} className="inline-block text-white bg-gray-500 hover:bg-gray-600 focus:ring-gray-200 focus:ring-3 rounded px-2.5 py-1.5 text-sm">{t('stravaRun.list.edit')}</Link>
                <span> </span>
                <button type="button" onClick={() => confirmDelete(stravaRun.runNumber!)} className="inline-block text-white bg-gray-500 hover:bg-gray-600 focus:ring-gray-200 focus:ring-3 rounded px-2.5 py-1.5 text-sm cursor-pointer">{t('stravaRun.list.delete')}</button>
              </div>
            </td>
          </tr>
          ))}
        </tbody>
      </table>
    </div>
    <Pagination page={stravaRuns.page} />
    </>)}
  </>);
}
