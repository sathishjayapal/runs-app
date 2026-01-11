import React, { useEffect } from 'react';
import { useTranslation } from 'react-i18next';
import { Link, useNavigate, useParams } from 'react-router';
import { handleServerError, setYupDefaults } from 'app/common/utils';
import { useForm } from 'react-hook-form';
import { yupResolver } from '@hookform/resolvers/yup';
import { RunAppUserDTO } from 'app/run-app-user/run-app-user-model';
import axios from 'axios';
import InputRow from 'app/common/input-row/input-row';
import useDocumentTitle from 'app/common/use-document-title';
import * as yup from 'yup';


function getSchema() {
  setYupDefaults();
  return yup.object({
    email: yup.string().emptyToNull().max(100).required(),
    password: yup.string().emptyToNull().max(100).required(),
    name: yup.string().emptyToNull().max(100).required()
  });
}

export default function RunAppUserEdit() {
  const { t } = useTranslation();
  useDocumentTitle(t('runAppUser.edit.headline'));

  const navigate = useNavigate();
  const params = useParams();
  const currentId = +params.id!;

  const useFormResult = useForm({
    resolver: yupResolver(getSchema()),
  });

  const prepareForm = async () => {
    try {
      const data = (await axios.get('/api/runAppUsers/' + currentId)).data;
      useFormResult.reset(data);
    } catch (error: any) {
      handleServerError(error, navigate);
    }
  };

  useEffect(() => {
    prepareForm();
  }, []);

  const updateRunAppUser = async (data: RunAppUserDTO) => {
    window.scrollTo(0, 0);
    try {
      await axios.put('/api/runAppUsers/' + currentId, data);
      navigate('/runAppUsers', {
            state: {
              msgSuccess: t('runAppUser.update.success')
            }
          });
    } catch (error: any) {
      handleServerError(error, navigate, useFormResult.setError, t);
    }
  };

  return (<>
    <div className="flex flex-wrap mb-6">
      <h1 className="grow text-3xl md:text-4xl font-medium mb-2">{t('runAppUser.edit.headline')}</h1>
      <div>
        <Link to="/runAppUsers" className="inline-block text-white bg-gray-500 hover:bg-gray-600 focus:ring-gray-200 focus:ring-4 rounded px-5 py-2">{t('runAppUser.edit.back')}</Link>
      </div>
    </div>
    <form onSubmit={useFormResult.handleSubmit(updateRunAppUser)} noValidate>
      <InputRow useFormResult={useFormResult} object="runAppUser" field="id" disabled={true} type="number" />
      <InputRow useFormResult={useFormResult} object="runAppUser" field="email" required={true} />
      <InputRow useFormResult={useFormResult} object="runAppUser" field="password" required={true} type="password" />
      <InputRow useFormResult={useFormResult} object="runAppUser" field="name" required={true} />
      <input type="submit" value={t('runAppUser.edit.headline')} className="inline-block text-white bg-blue-600 hover:bg-blue-700 focus:ring-blue-300  focus:ring-4 rounded px-5 py-2 cursor-pointer mt-6" />
    </form>
  </>);
}
