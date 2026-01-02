import React, { useEffect, useState } from 'react';
import { useTranslation } from 'react-i18next';
import { Link, useNavigate } from 'react-router';
import { handleServerError } from 'app/common/utils';
import { ShedlockDTO } from 'app/shedlock/shedlock-model';
import axios from 'axios';
import useDocumentTitle from 'app/common/use-document-title';


export default function ShedlockList() {
  const { t } = useTranslation();
  useDocumentTitle(t('shedlock.list.headline'));

  const [shedlocks, setShedlocks] = useState<ShedlockDTO[]>([]);
  const navigate = useNavigate();

  const getAllShedlocks = async () => {
    try {
      const response = await axios.get('/api/shedlocks');
      setShedlocks(response.data);
    } catch (error: any) {
      handleServerError(error, navigate);
    }
  };

  const confirmDelete = async (name: number) => {
    if (!confirm(t('delete.confirm'))) {
      return;
    }
    try {
      await axios.delete('/api/shedlocks/' + name);
      navigate('/shedlocks', {
            state: {
              msgInfo: t('shedlock.delete.success')
            }
          });
      getAllShedlocks();
    } catch (error: any) {
      handleServerError(error, navigate);
    }
  };

  useEffect(() => {
    getAllShedlocks();
  }, []);

  return (<>
    <div className="flex flex-wrap mb-6">
      <h1 className="grow text-3xl md:text-4xl font-medium mb-2">{t('shedlock.list.headline')}</h1>
      <div>
        <Link to="/shedlocks/add" className="inline-block text-white bg-blue-600 hover:bg-blue-700 focus:ring-blue-300  focus:ring-4 rounded px-5 py-2">{t('shedlock.list.createNew')}</Link>
      </div>
    </div>
    {!shedlocks || shedlocks.length === 0 ? (
    <div>{t('shedlock.list.empty')}</div>
    ) : (
    <div className="overflow-x-auto">
      <table className="w-full">
        <thead>
          <tr>
            <th scope="col" className="text-left p-2">{t('shedlock.name.label')}</th>
            <th scope="col" className="text-left p-2">{t('shedlock.lockUntil.label')}</th>
            <th scope="col" className="text-left p-2">{t('shedlock.lockedAt.label')}</th>
            <th></th>
          </tr>
        </thead>
        <tbody className="border-t-2 border-black">
          {shedlocks.map((shedlock) => (
          <tr key={shedlock.name} className="odd:bg-gray-100">
            <td className="p-2">{shedlock.name}</td>
            <td className="p-2">{shedlock.lockUntil}</td>
            <td className="p-2">{shedlock.lockedAt}</td>
            <td className="p-2">
              <div className="float-right whitespace-nowrap">
                <Link to={'/shedlocks/edit/' + shedlock.name} className="inline-block text-white bg-gray-500 hover:bg-gray-600 focus:ring-gray-200 focus:ring-3 rounded px-2.5 py-1.5 text-sm">{t('shedlock.list.edit')}</Link>
                <span> </span>
                <button type="button" onClick={() => confirmDelete(shedlock.name!)} className="inline-block text-white bg-gray-500 hover:bg-gray-600 focus:ring-gray-200 focus:ring-3 rounded px-2.5 py-1.5 text-sm cursor-pointer">{t('shedlock.list.delete')}</button>
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
