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
    'activityDate,ASC': t('garminRun.list.sort.activityDate,ASC')
  };

  const getAllGarminRuns = async () => {
    try {
      const response = await axios.get('/api/garminRuns?' + listParams);
      setGarminRuns(response.data);
    } catch (error: any) {
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
    {((garminRuns && garminRuns.page.totalElements !== 0) || searchParams.get('filter')) && (
    <div className="flex flex-wrap justify-between">
      <SearchFilter placeholder={t('garminRun.list.filter')} />
      <Sorting sortOptions={sortOptions} />
    </div>
    )}
    {!garminRuns || garminRuns.page.totalElements === 0 ? (
    <div>{t('garminRun.list.empty')}</div>
    ) : (<>
    <div className="overflow-x-auto">
      <table className="w-full">
        <thead>
          <tr>
            <th scope="col" className="text-left p-2">{t('garminRun.id.label')}</th>
            <th scope="col" className="text-left p-2">{t('garminRun.activityId.label')}</th>
            <th scope="col" className="text-left p-2">{t('garminRun.updatedBy.label')}</th>
            <th scope="col" className="text-left p-2">{t('garminRun.createdBy.label')}</th>
            <th></th>
          </tr>
        </thead>
        <tbody className="border-t-2 border-black">
          {garminRuns.content.map((garminRun) => (
          <tr key={garminRun.id} className="odd:bg-gray-100">
            <td className="p-2">{garminRun.id}</td>
            <td className="p-2">{garminRun.activityId}</td>
            <td className="p-2">{garminRun.updatedBy}</td>
            <td className="p-2">{garminRun.createdBy}</td>
            <td className="p-2">
              <div className="float-right whitespace-nowrap">
                <Link to={'/garminRuns/edit/' + garminRun.id} className="inline-block text-white bg-gray-500 hover:bg-gray-600 focus:ring-gray-200 focus:ring-3 rounded px-2.5 py-1.5 text-sm">{t('garminRun.list.edit')}</Link>
                <span> </span>
                <button type="button" onClick={() => confirmDelete(garminRun.id!)} className="inline-block text-white bg-gray-500 hover:bg-gray-600 focus:ring-gray-200 focus:ring-3 rounded px-2.5 py-1.5 text-sm cursor-pointer">{t('garminRun.list.delete')}</button>
              </div>
            </td>
          </tr>
          ))}
        </tbody>
      </table>
    </div>
    <Pagination page={garminRuns.page} />
    </>)}
  </>);
}
