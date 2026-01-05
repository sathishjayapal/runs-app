import React, { useEffect, useState } from 'react';
import { useTranslation } from 'react-i18next';
import { Link, useNavigate } from 'react-router';
import { handleServerError, setYupDefaults } from 'app/common/utils';
import { useForm } from 'react-hook-form';
import { yupResolver } from '@hookform/resolvers/yup';
import { StravaRunDTO } from 'app/strava-run/strava-run-model';
import axios from 'axios';
import InputRow from 'app/common/input-row/input-row';
import useDocumentTitle from 'app/common/use-document-title';
import * as yup from 'yup';


function getSchema() {
  setYupDefaults();
  return yup.object({
    customerId: yup.number().integer().emptyToNull().required(),
    runName: yup.string().emptyToNull().max(100).required(),
    runDate: yup.string().emptyToNull().required(),
    miles: yup.number().integer().emptyToNull().required(),
    startLocation: yup.number().integer().emptyToNull().required(),
    createdBy: yup.number().integer().emptyToNull().required(),
    updatedBy: yup.number().integer().emptyToNull().required()
  });
}

export default function StravaRunAdd() {
  const { t } = useTranslation();
  useDocumentTitle(t('stravaRun.add.headline'));

  const navigate = useNavigate();
  const [createdByValues, setCreatedByValues] = useState<Map<number,string>>(new Map());
  const [updatedByValues, setUpdatedByValues] = useState<Map<number,string>>(new Map());

  const useFormResult = useForm({
    resolver: yupResolver(getSchema()),
  });

  const prepareRelations = async () => {
    try {
      const createdByValuesResponse = await axios.get('/api/stravaRuns/createdByValues');
      setCreatedByValues(createdByValuesResponse.data);
      const updatedByValuesResponse = await axios.get('/api/stravaRuns/updatedByValues');
      setUpdatedByValues(updatedByValuesResponse.data);
    } catch (error: any) {
      handleServerError(error, navigate);
    }
  };

  useEffect(() => {
    prepareRelations();
  }, []);

  const createStravaRun = async (data: StravaRunDTO) => {
    window.scrollTo(0, 0);
    try {
      await axios.post('/api/stravaRuns', data);
      navigate('/stravaRuns', {
            state: {
              msgSuccess: t('stravaRun.create.success')
            }
          });
    } catch (error: any) {
      handleServerError(error, navigate, useFormResult.setError, t);
    }
  };

  return (<>
    <div className="flex flex-wrap mb-6">
      <h1 className="grow text-3xl md:text-4xl font-medium mb-2">{t('stravaRun.add.headline')}</h1>
      <div>
        <Link to="/stravaRuns" className="inline-block text-white bg-gray-500 hover:bg-gray-600 focus:ring-gray-200 focus:ring-4 rounded px-5 py-2">{t('stravaRun.add.back')}</Link>
      </div>
    </div>
    <form onSubmit={useFormResult.handleSubmit(createStravaRun)} noValidate>
      <InputRow useFormResult={useFormResult} object="stravaRun" field="customerId" required={true} type="number" />
      <InputRow useFormResult={useFormResult} object="stravaRun" field="runName" required={true} />
      <InputRow useFormResult={useFormResult} object="stravaRun" field="runDate" required={true} type="datepicker" />
      <InputRow useFormResult={useFormResult} object="stravaRun" field="miles" required={true} type="number" />
      <InputRow useFormResult={useFormResult} object="stravaRun" field="startLocation" required={true} type="number" />
      <InputRow useFormResult={useFormResult} object="stravaRun" field="createdBy" required={true} type="select" options={createdByValues} />
      <InputRow useFormResult={useFormResult} object="stravaRun" field="updatedBy" required={true} type="select" options={updatedByValues} />
      <input type="submit" value={t('stravaRun.add.headline')} className="inline-block text-white bg-blue-600 hover:bg-blue-700 focus:ring-blue-300  focus:ring-4 rounded px-5 py-2 cursor-pointer mt-6" />
    </form>
  </>);
}
