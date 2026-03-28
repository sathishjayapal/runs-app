import React, {useEffect, useState} from 'react';
import {useTranslation} from 'react-i18next';
import {useNavigate, useSearchParams} from 'react-router';
import {getListParams, handleServerError} from 'app/common/utils';
import {ShedlockDTO} from 'app/shedlock/shedlock-model';
import {PagedModel, Pagination} from 'app/common/list-helper/pagination';
import axios from 'axios';
import SearchFilter from 'app/common/list-helper/search-filter';
import Sorting from 'app/common/list-helper/sorting';
import useDocumentTitle from 'app/common/use-document-title';


export default function ShedlockList() {
  const { t } = useTranslation();
  useDocumentTitle(t('shedlock.list.headline'));

  const [shedlocks, setShedlocks] = useState<PagedModel<ShedlockDTO>|undefined>(undefined);
  const navigate = useNavigate();
  const [searchParams, ] = useSearchParams();
  const listParams = getListParams();
  const sortOptions = {
    'name,ASC': t('shedlock.list.sort.name,ASC'),
    'lockUntil,ASC': t('shedlock.list.sort.lockUntil,ASC'),
    'lockedAt,ASC': t('shedlock.list.sort.lockedAt,ASC')
  };
  const totalElements = shedlocks?.page?.totalElements ?? 0;

  const getAllShedlocks = async () => {
    try {
      const response = await axios.get('/api/shedlocks?' + listParams);
      setShedlocks(response.data);
    } catch (error: any) {
      if (error?.response?.status === 401) {
        window.location.href = '/login';
        return;
      }
      handleServerError(error, navigate);
    }
  };

  useEffect(() => {
    getAllShedlocks();
  }, [searchParams]);

  return (<>
    <div className="flex flex-wrap mb-6">
      <div className="grow">
        <h1 className="text-3xl md:text-4xl font-medium mb-2">{t('shedlock.list.headline')}</h1>
        <p className="text-gray-600 text-sm">Read-only view of distributed lock entries. Locks are managed automatically
          by the system.</p>
      </div>
    </div>
    {((totalElements !== 0 && shedlocks?.page) || searchParams.get('filter')) && (
    <div className="flex flex-wrap justify-between">
      <SearchFilter placeholder={t('shedlock.list.filter')} />
      <Sorting sortOptions={sortOptions} />
    </div>
    )}
    {!shedlocks || totalElements === 0 ? (
        <div className="bg-blue-50 border border-blue-200 rounded p-4">
          <p className="text-blue-800">{t('shedlock.list.empty')}</p>
          <p className="text-blue-600 text-sm mt-2">Locks appear here only while scheduled jobs are running. This table
            is usually empty.</p>
        </div>
    ) : (<>
    <div className="overflow-x-auto">
      <table className="w-full">
        <thead>
          <tr>
            <th scope="col" className="text-left p-2">{t('shedlock.name.label')}</th>
            <th scope="col" className="text-left p-2">{t('shedlock.lockUntil.label')}</th>
            <th scope="col" className="text-left p-2">{t('shedlock.lockedAt.label')}</th>
            <th scope="col" className="text-left p-2">{t('shedlock.lockedBy.label')}</th>
          </tr>
        </thead>
        <tbody className="border-t-2 border-black">
          {shedlocks.content.map((shedlock) => (
          <tr key={shedlock.name} className="odd:bg-gray-100">
            <td className="p-2 font-mono text-sm">{shedlock.name}</td>
            <td className="p-2 text-sm">{shedlock.lockUntil}</td>
            <td className="p-2 text-sm">{shedlock.lockedAt}</td>
            <td className="p-2 text-sm">{shedlock.lockedBy}</td>
          </tr>
          ))}
        </tbody>
      </table>
    </div>
    <Pagination page={shedlocks.page} />
    </>)}
  </>);
}
