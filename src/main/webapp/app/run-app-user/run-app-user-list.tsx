import React, {useEffect, useState} from 'react';
import {useTranslation} from 'react-i18next';
import {Link, useNavigate, useSearchParams} from 'react-router';
import {getListParams, handleServerError} from 'app/common/utils';
import {RunAppUserDTO} from 'app/run-app-user/run-app-user-model';
import {PagedModel, Pagination} from 'app/common/list-helper/pagination';
import axios from 'axios';
import SearchFilter from 'app/common/list-helper/search-filter';
import Sorting from 'app/common/list-helper/sorting';
import useDocumentTitle from 'app/common/use-document-title';


export default function RunAppUserList() {
  const { t } = useTranslation();
  useDocumentTitle(t('runAppUser.list.headline'));

  const [runAppUsers, setRunAppUsers] = useState<PagedModel<RunAppUserDTO>|undefined>(undefined);
  const [availableRoles, setAvailableRoles] = useState<Map<number, string>>(new Map());
  const navigate = useNavigate();
  const [searchParams, ] = useSearchParams();
  const listParams = getListParams();
  const sortOptions = {
    'id,ASC': t('runAppUser.list.sort.id,ASC'),
    'email,ASC': t('runAppUser.list.sort.email,ASC'),
    'name,ASC': t('runAppUser.list.sort.name,ASC')
  };
  const totalElements = runAppUsers?.page?.totalElements ?? 0;

  const fetchRoles = async () => {
    try {
      const response = await axios.get('/api/runnerAppRoles?size=100');
      const roleMap = new Map();
      response.data.content?.forEach((role: any) => {
        roleMap.set(role.id, role.roleName);
      });
      setAvailableRoles(roleMap);
    } catch (error: any) {
      console.error('Failed to fetch roles:', error);
    }
  };

  const getAllRunAppUsers = async () => {
    try {
      const response = await axios.get('/api/runAppUsers?' + listParams);
      setRunAppUsers(response.data);
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
    fetchRoles();
  }, []);

  useEffect(() => {
    getAllRunAppUsers();
  }, [searchParams]);

  return (<>
    <div className="flex flex-wrap mb-6">
      <h1 className="grow text-3xl md:text-4xl font-medium mb-2">{t('runAppUser.list.headline')}</h1>
      <div>
        <Link to="/runAppUsers/add"
              className="inline-block text-white bg-blue-600 hover:bg-blue-700 focus:ring-blue-300 focus:ring-4 rounded px-5 py-2">{t('runAppUser.list.createNew')}</Link>
      </div>
    </div>
    {((totalElements !== 0 && runAppUsers?.page) || searchParams.get('filter')) && (
    <div className="flex flex-wrap justify-between">
      <SearchFilter placeholder={t('runAppUser.list.filter')} />
      <Sorting sortOptions={sortOptions} />
    </div>
    )}
    {!runAppUsers || totalElements === 0 ? (
    <div>{t('runAppUser.list.empty')}</div>
    ) : (<>
    <div className="overflow-x-auto">
      <table className="w-full border-collapse">
        <thead>
        <tr className="bg-gray-100">
          <th scope="col" className="border border-gray-300 px-4 py-2 text-left">{t('runAppUser.id.label')}</th>
          <th scope="col" className="border border-gray-300 px-4 py-2 text-left">{t('runAppUser.email.label')}</th>
          <th scope="col" className="border border-gray-300 px-4 py-2 text-left">{t('runAppUser.name.label')}</th>
          <th scope="col" className="border border-gray-300 px-4 py-2 text-left">{t('runAppUser.roles.label')}</th>
          <th className="border border-gray-300 px-4 py-2 text-center"></th>
          </tr>
        </thead>
        <tbody>
          {runAppUsers.content.map((runAppUser) => (
              <tr key={runAppUser.id} className="hover:bg-gray-50">
                <td className="border border-gray-300 px-4 py-2 font-mono text-sm">{runAppUser.id}</td>
                <td className="border border-gray-300 px-4 py-2">{runAppUser.email}</td>
                <td className="border border-gray-300 px-4 py-2">{runAppUser.name}</td>
                <td className="border border-gray-300 px-4 py-2">
              {runAppUser.roles?.map(roleId => availableRoles.get(roleId)).filter(Boolean).join(', ') || '-'}
            </td>
                <td className="border border-gray-300 px-4 py-2 text-center">
                  <div className="flex gap-2 justify-center">
                    <Link to={'/runAppUsers/edit/' + runAppUser.id}
                          className="inline-flex items-center text-white bg-blue-600 hover:bg-blue-700 focus:ring-4 focus:ring-blue-300 rounded px-3 py-1.5 text-sm font-medium transition-colors">
                      <svg className="w-4 h-4 mr-1" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                        <path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2"
                              d="M11 5H6a2 2 0 00-2 2v11a2 2 0 002 2h11a2 2 0 002-2v-5m-1.414-9.414a2 2 0 112.828 2.828L11.828 15H9v-2.828l8.586-8.586z"/>
                      </svg>
                      {t('runAppUser.list.edit')}
                    </Link>
                    <button type="button" onClick={() => confirmDelete(runAppUser.id!)}
                            className="inline-flex items-center text-white bg-red-600 hover:bg-red-700 focus:ring-4 focus:ring-red-300 rounded px-3 py-1.5 text-sm font-medium cursor-pointer transition-colors">
                      <svg className="w-4 h-4 mr-1" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                        <path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2"
                              d="M19 7l-.867 12.142A2 2 0 0116.138 21H7.862a2 2 0 01-1.995-1.858L5 7m5 4v6m4-6v6m1-10V4a1 1 0 00-1-1h-4a1 1 0 00-1 1v3M4 7h16"/>
                      </svg>
                      {t('runAppUser.list.delete')}
                    </button>
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
