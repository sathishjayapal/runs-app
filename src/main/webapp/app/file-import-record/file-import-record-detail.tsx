import React, {useEffect, useState} from 'react';
import {Link, useNavigate, useParams} from 'react-router';
import {useTranslation} from 'react-i18next';
import useDocumentTitle from 'app/common/use-document-title';
import {handleServerError} from 'app/common/utils';
import axios from 'axios';
import type {FileImportRecord} from './file-import-record-model';


export default function FileImportRecordDetail() {
  const {t} = useTranslation();
  const {id} = useParams();
  const navigate = useNavigate();
  useDocumentTitle(t('fileImportRecord.detail.headline'));
  const [data, setData] = useState<FileImportRecord>();

  const getRecord = async () => {
    try {
      const response = await axios.get(`/api/file-import-records/${id}`);
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
    getRecord();
  }, [id]);

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
    <div className="flex items-center justify-between mb-8">
      <h1 className="text-3xl md:text-4xl font-medium">{t('fileImportRecord.detail.headline')}</h1>
      <Link to="/fileImportRecords" className="text-blue-600 hover:underline">
        {t('fileImportRecord.detail.back')}
      </Link>
    </div>
    {data && (
      <div className="bg-white border border-gray-300 rounded-lg p-6">
        <div className="grid grid-cols-1 md:grid-cols-2 gap-6">
          <div>
            <h3 className="text-lg font-semibold mb-4">File Information</h3>
            <div className="space-y-3">
              <div>
                <label className="text-sm text-gray-600">{t('fileImportRecord.fileName.label')}</label>
                <p className="font-medium break-all">{data.fileName}</p>
              </div>
              <div>
                <label className="text-sm text-gray-600">{t('fileImportRecord.processedAt.label')}</label>
                <p className="font-medium">{new Date(data.processedAt).toLocaleString()}</p>
              </div>
              <div>
                <label className="text-sm text-gray-600">{t('fileImportRecord.completedAt.label')}</label>
                <p className="font-medium">{new Date(data.completedAt).toLocaleString()}</p>
              </div>
            </div>
          </div>

          <div>
            <h3 className="text-lg font-semibold mb-4">Processing Status</h3>
            <div className="space-y-3">
              <div>
                <label className="text-sm text-gray-600">{t('fileImportRecord.status.label')}</label>
                <p>
                  <span className={`inline-block px-3 py-1 rounded font-semibold ${getStatusBadgeClass(data.status)}`}>
                    {data.status}
                  </span>
                </p>
              </div>
              <div>
                <label className="text-sm text-gray-600">{t('fileImportRecord.reconciliationStatus.label')}</label>
                <p>
                  <span className={`inline-block px-3 py-1 rounded font-semibold ${getReconciliationBadgeClass(data.reconciliationStatus)}`}>
                    {data.reconciliationStatus}
                  </span>
                </p>
              </div>
            </div>
          </div>
        </div>

        <div className="mt-6 pt-6 border-t border-gray-200">
          <h3 className="text-lg font-semibold mb-4">Processing Statistics</h3>
          <div className="grid grid-cols-2 md:grid-cols-4 gap-4">
            <div className="bg-gray-50 p-4 rounded">
              <label className="text-sm text-gray-600">{t('fileImportRecord.expectedRows.label')}</label>
              <p className="text-2xl font-bold text-gray-800">{data.expectedRows}</p>
            </div>
            <div className="bg-green-50 p-4 rounded">
              <label className="text-sm text-green-600">{t('fileImportRecord.successCount.label')}</label>
              <p className="text-2xl font-bold text-green-700">{data.successCount}</p>
            </div>
            <div className="bg-red-50 p-4 rounded">
              <label className="text-sm text-red-600">{t('fileImportRecord.failedCount.label')}</label>
              <p className="text-2xl font-bold text-red-700">{data.failedCount}</p>
            </div>
            <div className="bg-yellow-50 p-4 rounded">
              <label className="text-sm text-yellow-600">{t('fileImportRecord.skippedCount.label')}</label>
              <p className="text-2xl font-bold text-yellow-700">{data.skippedCount}</p>
            </div>
          </div>
        </div>

        {data.reconciliationReport && (
          <div className="mt-6 pt-6 border-t border-gray-200">
            <h3 className="text-lg font-semibold mb-2">Reconciliation Report</h3>
            <div className="bg-gray-50 p-4 rounded">
              <pre className="text-sm whitespace-pre-wrap">{data.reconciliationReport}</pre>
            </div>
          </div>
        )}
      </div>
    )}
  </>);
}
