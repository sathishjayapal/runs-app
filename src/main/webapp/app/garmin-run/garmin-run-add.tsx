import React, { useEffect, useState } from 'react';
import { useTranslation } from 'react-i18next';
import { Link, useNavigate } from 'react-router';
import { handleServerError, setYupDefaults } from 'app/common/utils';
import { useForm } from 'react-hook-form';
import { yupResolver } from '@hookform/resolvers/yup';
import { GarminRunDTO } from 'app/garmin-run/garmin-run-model';
import axios from 'axios';
import InputRow from 'app/common/input-row/input-row';
import useDocumentTitle from 'app/common/use-document-title';
import * as yup from 'yup';


function getSchema() {
  setYupDefaults();
  return yup.object({
    activityId: yup.string().emptyToNull().numeric(10, 2).required(),
    activityDate: yup.string().emptyToNull().required(),
    activityType: yup.string().emptyToNull().required(),
    activityName: yup.string().emptyToNull().required(),
    activityDescription: yup.string().emptyToNull(),
    elapsedTime: yup.string().emptyToNull(),
    distance: yup.string().emptyToNull().required(),
    maxHeartRate: yup.string().emptyToNull(),
    calories: yup.string().emptyToNull(),
    createdAt: yup.string().emptyToNull().offsetDateTime().required(),
    updatedAt: yup.string().emptyToNull().offsetDateTime(),
    updatedBy: yup.string().emptyToNull().max(40),
    createdBy: yup.number().integer().emptyToNull().required()
  });
}

export default function GarminRunAdd() {
  const { t } = useTranslation();
  useDocumentTitle(t('garminRun.add.headline'));

  const navigate = useNavigate();
  const [createdByValues, setCreatedByValues] = useState<Map<number,string>>(new Map());

  const useFormResult = useForm({
    resolver: yupResolver(getSchema()),
  });

  const prepareRelations = async () => {
    try {
      const createdByValuesResponse = await axios.get('/api/garminRuns/createdByValues');
      setCreatedByValues(createdByValuesResponse.data);
    } catch (error: any) {
      handleServerError(error, navigate);
    }
  };

  useEffect(() => {
    prepareRelations();
  }, []);

  const createGarminRun = async (data: GarminRunDTO) => {
    window.scrollTo(0, 0);
    try {
      await axios.post('/api/garminRuns', data);
      navigate('/garminRuns', {
            state: {
              msgSuccess: t('garminRun.create.success')
            }
          });
    } catch (error: any) {
      handleServerError(error, navigate, useFormResult.setError, t);
    }
  };

  return (<>
    <div className="flex flex-wrap mb-6">
      <h1 className="grow text-3xl md:text-4xl font-medium mb-2">{t('garminRun.add.headline')}</h1>
      <div>
        <Link to="/garminRuns" className="inline-block text-white bg-gray-500 hover:bg-gray-600 focus:ring-gray-200 focus:ring-4 rounded px-5 py-2">{t('garminRun.add.back')}</Link>
      </div>
    </div>
    <form onSubmit={useFormResult.handleSubmit(createGarminRun)} noValidate>
      <InputRow useFormResult={useFormResult} object="garminRun" field="activityId" required={true} />
      <InputRow useFormResult={useFormResult} object="garminRun" field="activityDate" required={true} type="textarea" />
      <InputRow useFormResult={useFormResult} object="garminRun" field="activityType" required={true} type="textarea" />
      <InputRow useFormResult={useFormResult} object="garminRun" field="activityName" required={true} type="textarea" />
      <InputRow useFormResult={useFormResult} object="garminRun" field="activityDescription" type="textarea" />
      <InputRow useFormResult={useFormResult} object="garminRun" field="elapsedTime" type="textarea" />
      <InputRow useFormResult={useFormResult} object="garminRun" field="distance" required={true} type="textarea" />
      <InputRow useFormResult={useFormResult} object="garminRun" field="maxHeartRate" type="textarea" />
      <InputRow useFormResult={useFormResult} object="garminRun" field="calories" type="textarea" />
      <InputRow useFormResult={useFormResult} object="garminRun" field="createdAt" required={true} />
      <InputRow useFormResult={useFormResult} object="garminRun" field="updatedAt" />
      <InputRow useFormResult={useFormResult} object="garminRun" field="updatedBy" />
      <InputRow useFormResult={useFormResult} object="garminRun" field="createdBy" required={true} type="select" options={createdByValues} />
      <input type="submit" value={t('garminRun.add.headline')} className="inline-block text-white bg-blue-600 hover:bg-blue-700 focus:ring-blue-300  focus:ring-4 rounded px-5 py-2 cursor-pointer mt-6" />
    </form>
  </>);
}
