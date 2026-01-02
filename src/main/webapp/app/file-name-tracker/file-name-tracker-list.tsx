import React, { useEffect, useState } from 'react';
import { useTranslation } from 'react-i18next';
import { Link, useNavigate } from 'react-router';
import { handleServerError } from 'app/common/utils';
import { FileNameTrackerDTO } from 'app/file-name-tracker/file-name-tracker-model';
import axios from 'axios';
import useDocumentTitle from 'app/common/use-document-title';


export default function FileNameTrackerList() {
  const { t } = useTranslation();
  useDocumentTitle(t('fileNameTracker.list.headline'));

  const [fileNameTrackers, setFileNameTrackers] = useState<FileNameTrackerDTO[]>([]);
  const navigate = useNavigate();

  const getAllFileNameTrackers = async () => {
    try {
      const response = await axios.get('/api/fileNameTrackers');
      setFileNameTrackers(response.data);
    } catch (error: any) {
      handleServerError(error, navigate);
    }
  };

  const confirmDelete = async (id: number) => {
    if (!confirm(t('delete.confirm'))) {
      return;
    }
    try {
      await axios.delete('/api/fileNameTrackers/' + id);
      navigate('/fileNameTrackers', {
            state: {
              msgInfo: t('fileNameTracker.delete.success')
            }
          });
      getAllFileNameTrackers();
    } catch (error: any) {
      handleServerError(error, navigate);
    }
  };

  useEffect(() => {
    getAllFileNameTrackers();
  }, []);

  return (<>
    <div className="flex flex-wrap mb-6">
      <h1 className="grow text-3xl md:text-4xl font-medium mb-2">{t('fileNameTracker.list.headline')}</h1>
      <div>
        <Link to="/fileNameTrackers/add" className="inline-block text-white bg-blue-600 hover:bg-blue-700 focus:ring-blue-300  focus:ring-4 rounded px-5 py-2">{t('fileNameTracker.list.createNew')}</Link>
      </div>
    </div>
    {!fileNameTrackers || fileNameTrackers.length === 0 ? (
    <div>{t('fileNameTracker.list.empty')}</div>
    ) : (
    <div className="overflow-x-auto">
      <table className="w-full">
        <thead>
          <tr>
            <th scope="col" className="text-left p-2">{t('fileNameTracker.id.label')}</th>
            <th scope="col" className="text-left p-2">{t('fileNameTracker.createdAt.label')}</th>
            <th scope="col" className="text-left p-2">{t('fileNameTracker.updatedAt.label')}</th>
            <th scope="col" className="text-left p-2">{t('fileNameTracker.updatedBy.label')}</th>
            <th scope="col" className="text-left p-2">{t('fileNameTracker.createdBy.label')}</th>
            <th></th>
          </tr>
        </thead>
        <tbody className="border-t-2 border-black">
          {fileNameTrackers.map((fileNameTracker) => (
          <tr key={fileNameTracker.id} className="odd:bg-gray-100">
            <td className="p-2">{fileNameTracker.id}</td>
            <td className="p-2">{fileNameTracker.createdAt}</td>
            <td className="p-2">{fileNameTracker.updatedAt}</td>
            <td className="p-2">{fileNameTracker.updatedBy}</td>
            <td className="p-2">{fileNameTracker.createdBy}</td>
            <td className="p-2">
              <div className="float-right whitespace-nowrap">
                <Link to={'/fileNameTrackers/edit/' + fileNameTracker.id} className="inline-block text-white bg-gray-500 hover:bg-gray-600 focus:ring-gray-200 focus:ring-3 rounded px-2.5 py-1.5 text-sm">{t('fileNameTracker.list.edit')}</Link>
                <span> </span>
                <button type="button" onClick={() => confirmDelete(fileNameTracker.id!)} className="inline-block text-white bg-gray-500 hover:bg-gray-600 focus:ring-gray-200 focus:ring-3 rounded px-2.5 py-1.5 text-sm cursor-pointer">{t('fileNameTracker.list.delete')}</button>
              </div>
            </td>
          </tr>
          ))}
        </tbody>
      </table>
    </div>
    )}
  </>);
}
