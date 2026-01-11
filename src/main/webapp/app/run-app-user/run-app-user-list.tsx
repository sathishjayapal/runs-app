import React, { useEffect, useState } from 'react';
import { useTranslation } from 'react-i18next';
import { Link, useNavigate, useSearchParams } from 'react-router';
import { handleServerError, getListParams } from 'app/common/utils';
import { RunAppUserDTO } from 'app/run-app-user/run-app-user-model';
import { PagedModel, Pagination } from 'app/common/list-helper/pagination';
import axios from 'axios';
import SearchFilter from 'app/common/list-helper/search-filter';
import Sorting from 'app/common/list-helper/sorting';
import useDocumentTitle from 'app/common/use-document-title';


export default function RunAppUserList() {
  const { t } = useTranslation();
  useDocumentTitle(t('runAppUser.list.headline'));

  const [runAppUsers, setRunAppUsers] = useState<PagedModel<RunAppUserDTO>|undefined>(undefined);
  const navigate = useNavigate();
  const [searchParams, ] = useSearchParams();
  const listParams = getListParams();
  const sortOptions = {
    'id,ASC': t('runAppUser.list.sort.id,ASC'), 
    'email,ASC': t('runAppUser.list.sort.email,ASC'), 
    'name,ASC': t('runAppUser.list.sort.name,ASC')
  };

  const getAllRunAppUsers = async () => {
    try {
      const response = await axios.get('/api/runAppUsers?' + listParams);
      setRunAppUsers(response.data);
    } catch (error: any) {
      handleServerError(error, navigate);
    }
  };

  const confirmDelete = async (id: number) => {
    if (!confirm(t('delete.confirm'))) {
      return;
    }
    try {
      await axios.delete('/api/runAppUsers/' + id);
      navigate('/runAppUsers', {
            state: {
              msgInfo: t('runAppUser.delete.success')
            }
          });
      getAllRunAppUsers();
    } catch (error: any) {
      if (error?.response?.data?.code === 'REFERENCED') {
        const messageParts = error.response.data.message.split(',');
        navigate('/runAppUsers', {
              state: {
                msgError: t(messageParts[0]!, { id: messageParts[1]! })
              }
            });
        return;
      }
      handleServerError(error, navigate);
    }
  };

  useEffect(() => {
    getAllRunAppUsers();
  }, [searchParams]);

  return (<>
    <div className="flex flex-wrap mb-6">
      <h1 className="grow text-3xl md:text-4xl font-medium mb-2">{t('runAppUser.list.headline')}</h1>
      <div>
        <Link to="/runAppUsers/add" className="inline-block text-white bg-blue-600 hover:bg-blue-700 focus:ring-blue-300  focus:ring-4 rounded px-5 py-2">{t('runAppUser.list.createNew')}</Link>
      </div>
    </div>
    {((runAppUsers && runAppUsers.page.totalElements !== 0) || searchParams.get('filter')) && (
    <div className="flex flex-wrap justify-between">
      <SearchFilter placeholder={t('runAppUser.list.filter')} />
      <Sorting sortOptions={sortOptions} />
    </div>
    )}
    {!runAppUsers || runAppUsers.page.totalElements === 0 ? (
    <div>{t('runAppUser.list.empty')}</div>
    ) : (<>
    <div className="overflow-x-auto">
      <table className="w-full">
        <thead>
          <tr>
            <th scope="col" className="text-left p-2">{t('runAppUser.id.label')}</th>
            <th scope="col" className="text-left p-2">{t('runAppUser.email.label')}</th>
            <th scope="col" className="text-left p-2">{t('runAppUser.name.label')}</th>
            <th></th>
          </tr>
        </thead>
        <tbody className="border-t-2 border-black">
          {runAppUsers.content.map((runAppUser) => (
          <tr key={runAppUser.id} className="odd:bg-gray-100">
            <td className="p-2">{runAppUser.id}</td>
            <td className="p-2">{runAppUser.email}</td>
            <td className="p-2">{runAppUser.name}</td>
            <td className="p-2">
              <div className="float-right whitespace-nowrap">
                <Link to={'/runAppUsers/edit/' + runAppUser.id} className="inline-block text-white bg-gray-500 hover:bg-gray-600 focus:ring-gray-200 focus:ring-3 rounded px-2.5 py-1.5 text-sm">{t('runAppUser.list.edit')}</Link>
                <span> </span>
                <button type="button" onClick={() => confirmDelete(runAppUser.id!)} className="inline-block text-white bg-gray-500 hover:bg-gray-600 focus:ring-gray-200 focus:ring-3 rounded px-2.5 py-1.5 text-sm cursor-pointer">{t('runAppUser.list.delete')}</button>
              </div>
            </td>
          </tr>
          ))}
        </tbody>
      </table>
    </div>
    <Pagination page={runAppUsers.page} />
    </>)}
  </>);
}
