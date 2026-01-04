import React from 'react';
import { useTranslation } from 'react-i18next';
import { Link, useNavigate } from 'react-router';
import { handleServerError, setYupDefaults } from 'app/common/utils';
import { useForm } from 'react-hook-form';
import { yupResolver } from '@hookform/resolvers/yup';
import { ShedlockDTO } from 'app/shedlock/shedlock-model';
import axios from 'axios';
import InputRow from 'app/common/input-row/input-row';
import useDocumentTitle from 'app/common/use-document-title';
import * as yup from 'yup';


function getSchema() {
  setYupDefaults();
  return yup.object({
    lockUntil: yup.string().emptyToNull().offsetDateTime().required(),
    lockedAt: yup.string().emptyToNull().offsetDateTime().required(),
    lockedBy: yup.string().emptyToNull().required()
  });
}

export default function ShedlockAdd() {
  const { t } = useTranslation();
  useDocumentTitle(t('shedlock.add.headline'));

  const navigate = useNavigate();

  const useFormResult = useForm({
    resolver: yupResolver(getSchema()),
  });

  const createShedlock = async (data: ShedlockDTO) => {
    window.scrollTo(0, 0);
    try {
      await axios.post('/api/shedlocks', data);
      navigate('/shedlocks', {
            state: {
              msgSuccess: t('shedlock.create.success')
            }
          });
    } catch (error: any) {
      handleServerError(error, navigate, useFormResult.setError, t);
    }
  };

  return (<>
    <div className="flex flex-wrap mb-6">
      <h1 className="grow text-3xl md:text-4xl font-medium mb-2">{t('shedlock.add.headline')}</h1>
      <div>
        <Link to="/shedlocks" className="inline-block text-white bg-gray-500 hover:bg-gray-600 focus:ring-gray-200 focus:ring-4 rounded px-5 py-2">{t('shedlock.add.back')}</Link>
      </div>
    </div>
    <form onSubmit={useFormResult.handleSubmit(createShedlock)} noValidate>
      <InputRow useFormResult={useFormResult} object="shedlock" field="lockUntil" required={true} />
      <InputRow useFormResult={useFormResult} object="shedlock" field="lockedAt" required={true} />
      <InputRow useFormResult={useFormResult} object="shedlock" field="lockedBy" required={true} type="textarea" />
      <input type="submit" value={t('shedlock.add.headline')} className="inline-block text-white bg-blue-600 hover:bg-blue-700 focus:ring-blue-300  focus:ring-4 rounded px-5 py-2 cursor-pointer mt-6" />
    </form>
  </>);
}
