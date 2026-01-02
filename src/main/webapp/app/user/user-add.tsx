import React from 'react';
import { useTranslation } from 'react-i18next';
import { Link, useNavigate } from 'react-router';
import { handleServerError, setYupDefaults } from 'app/common/utils';
import { useForm } from 'react-hook-form';
import { yupResolver } from '@hookform/resolvers/yup';
import { UserDTO } from 'app/user/user-model';
import axios from 'axios';
import InputRow from 'app/common/input-row/input-row';
import useDocumentTitle from 'app/common/use-document-title';
import * as yup from 'yup';


function getSchema() {
  setYupDefaults();
  return yup.object({
    email: yup.string().emptyToNull().max(100).required(),
    password: yup.string().emptyToNull().max(100).required(),
    name: yup.string().emptyToNull().max(100).required(),
    role: yup.string().emptyToNull().max(20).required(),
    createdAt: yup.string().emptyToNull().offsetDateTime().required()
  });
}

export default function UserAdd() {
  const { t } = useTranslation();
  useDocumentTitle(t('user.add.headline'));

  const navigate = useNavigate();

  const useFormResult = useForm({
    resolver: yupResolver(getSchema()),
  });

  const createUser = async (data: UserDTO) => {
    window.scrollTo(0, 0);
    try {
      await axios.post('/api/users', data);
      navigate('/users', {
            state: {
              msgSuccess: t('user.create.success')
            }
          });
    } catch (error: any) {
      handleServerError(error, navigate, useFormResult.setError, t);
    }
  };

  return (<>
    <div className="flex flex-wrap mb-6">
      <h1 className="grow text-3xl md:text-4xl font-medium mb-2">{t('user.add.headline')}</h1>
      <div>
        <Link to="/users" className="inline-block text-white bg-gray-500 hover:bg-gray-600 focus:ring-gray-200 focus:ring-4 rounded px-5 py-2">{t('user.add.back')}</Link>
      </div>
    </div>
    <form onSubmit={useFormResult.handleSubmit(createUser)} noValidate>
      <InputRow useFormResult={useFormResult} object="user" field="email" required={true} />
      <InputRow useFormResult={useFormResult} object="user" field="password" required={true} type="password" />
      <InputRow useFormResult={useFormResult} object="user" field="name" required={true} />
      <InputRow useFormResult={useFormResult} object="user" field="role" required={true} />
      <InputRow useFormResult={useFormResult} object="user" field="createdAt" required={true} />
      <input type="submit" value={t('user.add.headline')} className="inline-block text-white bg-blue-600 hover:bg-blue-700 focus:ring-blue-300  focus:ring-4 rounded px-5 py-2 cursor-pointer mt-6" />
    </form>
  </>);
}
