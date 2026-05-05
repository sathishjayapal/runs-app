import React, {useEffect, useState} from 'react';
import {Link, useNavigate, useSearchParams} from 'react-router';
import {useTranslation} from 'react-i18next';
import useDocumentTitle from 'app/common/use-document-title';
import {handleServerError, getListParams} from 'app/common/utils';
import type {FileImportRecord} from './file-import-record-model';
import {PagedModel, Pagination} from 'app/common/list-helper/pagination';
import axios from 'axios';
import Sorting from 'app/common/list-helper/sorting';


export default function FileImportRecordList() {
  const {t} = useTranslation();
  useDocumentTitle(t('fileImportRecord.list.headline'));
  const navigate = useNavigate();
  const [searchParams] = useSearchParams();
  const [data, setData] = useState<PagedModel<FileImportRecord>>();
  const listParams = getListParams();
  const sortOptions = {
    'processedAt,DESC': t('fileImportRecord.list.sort.processedAt,DESC'),
    'processedAt,ASC': t('fileImportRecord.list.sort.processedAt,ASC'),
    'fileName,ASC': t('fileImportRecord.list.sort.fileName,ASC')
  };
  const totalElements = data?.page?.totalElements ?? 0;
  const content = data?.content ?? [];

  const getAllRecords = async () => {
    try {
      const response = await axios.get('/api/file-import-records?' + listParams);
      setData(response.data);
    } catch (error: any) {
      if (error?.response?.status === 401) {
        window.location.href = '/login';
        return;
      }
      handleServerError(error, navigate);
    }
  };

  useEffect(() => {
    getAllRecords();
  }, [searchParams]);

  const getStatusBadgeClass = (status: string) => {
    switch (status) {
      case 'COMPLETE_SUCCESS':
        return 'bg-green-100 text-green-800';
      case 'COMPLETE_WITH_FAILURES':
        return 'bg-yellow-100 text-yellow-800';
      case 'FAILED':
        return 'bg-red-100 text-red-800';
      case 'PROCESSING':
        return 'bg-blue-100 text-blue-800';
      default:
        return 'bg-gray-100 text-gray-800';
    }
  };

  const getReconciliationBadgeClass = (status: string) => {
    switch (status) {
      case 'PASS':
        return 'bg-green-100 text-green-800';
      case 'PARTIAL_PASS':
        return 'bg-yellow-100 text-yellow-800';
      case 'FAIL':
        return 'bg-red-100 text-red-800';
      default:
        return 'bg-gray-100 text-gray-800';
    }
  };

  return (<>
    <h1 className="grow text-3xl md:text-4xl font-medium mb-8">{t('fileImportRecord.list.headline')}</h1>
    {totalElements === 0 &&
        <div className="bg-blue-50 border border-blue-200 text-blue-800 px-4 py-3 mb-4 rounded">{t('fileImportRecord.list.empty')}</div>}
    {totalElements > 0 && (<>
      <div className="mb-4">
        <Sorting sortOptions={sortOptions} />
      </div>
      <div className="overflow-x-auto">
        <table className="w-full border-collapse">
          <thead>
          <tr className="bg-gray-100">
            <th className="border border-gray-300 px-4 py-2 text-left">{t('fileImportRecord.fileName.label')}</th>
            <th className="border border-gray-300 px-4 py-2 text-center">{t('fileImportRecord.expectedRows.label')}</th>
            <th className="border border-gray-300 px-4 py-2 text-center">{t('fileImportRecord.successCount.label')}</th>
            <th className="border border-gray-300 px-4 py-2 text-center">{t('fileImportRecord.failedCount.label')}</th>
            <th className="border border-gray-300 px-4 py-2 text-center">{t('fileImportRecord.skippedCount.label')}</th>
            <th className="border border-gray-300 px-4 py-2 text-center">{t('fileImportRecord.status.label')}</th>
            <th className="border border-gray-300 px-4 py-2 text-center">{t('fileImportRecord.reconciliationStatus.label')}</th>
            <th className="border border-gray-300 px-4 py-2 text-center">{t('fileImportRecord.processedAt.label')}</th>
          </tr>
          </thead>
          <tbody>
          {content.map(record => (
              <tr key={record.id} className="hover:bg-gray-50">
                <td className="border border-gray-300 px-4 py-2">
                  <Link to={`/fileImportRecords/${record.id}`} className="text-blue-600 hover:underline">
                    {record.fileName}
                  </Link>
                </td>
                <td className="border border-gray-300 px-4 py-2 text-center">{record.expectedRows}</td>
                <td className="border border-gray-300 px-4 py-2 text-center text-green-600 font-semibold">{record.successCount}</td>
                <td className="border border-gray-300 px-4 py-2 text-center text-red-600 font-semibold">{record.failedCount}</td>
                <td className="border border-gray-300 px-4 py-2 text-center text-yellow-600 font-semibold">{record.skippedCount}</td>
                <td className="border border-gray-300 px-4 py-2 text-center">
                  <span className={`inline-block px-2 py-1 rounded text-xs font-semibold ${getStatusBadgeClass(record.status)}`}>
                    {record.status}
                  </span>
                </td>
                <td className="border border-gray-300 px-4 py-2 text-center">
                  <span className={`inline-block px-2 py-1 rounded text-xs font-semibold ${getReconciliationBadgeClass(record.reconciliationStatus)}`}>
                    {record.reconciliationStatus}
                  </span>
                </td>
                <td className="border border-gray-300 px-4 py-2 text-center text-sm">
                  {new Date(record.processedAt).toLocaleString()}
                </td>
              </tr>
          ))}
          </tbody>
        </table>
      </div>
      <Pagination page={data?.page}/>
    </>)}
  </>);
}
