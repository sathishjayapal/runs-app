import React, { useEffect, useState } from 'react';
import { useTranslation } from 'react-i18next';
import { Link, useNavigate } from 'react-router';
import { handleServerError } from 'app/common/utils';
import { GarminRunDTO } from 'app/garmin-run/garmin-run-model';
import axios from 'axios';
import useDocumentTitle from 'app/common/use-document-title';


export default function GarminRunList() {
  const { t } = useTranslation();
  useDocumentTitle(t('garminRun.list.headline'));

  const [garminRuns, setGarminRuns] = useState<GarminRunDTO[]>([]);
  const navigate = useNavigate();

  const getAllGarminRuns = async () => {
    try {
      const response = await axios.get('/api/garminRuns');
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
  }, []);

  return (<>
    <div className="flex flex-wrap mb-6">
      <h1 className="grow text-3xl md:text-4xl font-medium mb-2">{t('garminRun.list.headline')}</h1>
      <div>
        <Link to="/garminRuns/add" className="inline-block text-white bg-blue-600 hover:bg-blue-700 focus:ring-blue-300  focus:ring-4 rounded px-5 py-2">{t('garminRun.list.createNew')}</Link>
      </div>
    </div>
    {!garminRuns || garminRuns.length === 0 ? (
    <div>{t('garminRun.list.empty')}</div>
    ) : (
    <div className="overflow-x-auto">
      <table className="w-full">
        <thead>
          <tr>
            <th scope="col" className="text-left p-2">{t('garminRun.id.label')}</th>
            <th scope="col" className="text-left p-2">{t('garminRun.activityId.label')}</th>
            <th scope="col" className="text-left p-2">{t('garminRun.createdAt.label')}</th>
            <th scope="col" className="text-left p-2">{t('garminRun.updatedAt.label')}</th>
            <th scope="col" className="text-left p-2">{t('garminRun.updatedBy.label')}</th>
            <th scope="col" className="text-left p-2">{t('garminRun.createdBy.label')}</th>
            <th></th>
          </tr>
        </thead>
        <tbody className="border-t-2 border-black">
          {garminRuns.map((garminRun) => (
          <tr key={garminRun.id} className="odd:bg-gray-100">
            <td className="p-2">{garminRun.id}</td>
            <td className="p-2">{garminRun.activityId}</td>
            <td className="p-2">{garminRun.createdAt}</td>
            <td className="p-2">{garminRun.updatedAt}</td>
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
    )}
  </>);
}
