import React, { useEffect, useState } from 'react';
import { useTranslation } from 'react-i18next';
import { Link, useNavigate, useParams } from 'react-router';
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
    createdAt: yup.string().emptyToNull().required(),
    updatedAt: yup.string().emptyToNull(),
    updatedBy: yup.string().emptyToNull().max(20),
    createdBy: yup.number().integer().emptyToNull().required()
  });
}

export default function StravaRunEdit() {
  const { t } = useTranslation();
  useDocumentTitle(t('stravaRun.edit.headline'));

  const navigate = useNavigate();
  const [createdByValues, setCreatedByValues] = useState<Map<number,string>>(new Map());
  const params = useParams();
  const currentRunNumber = +params.runNumber!;

  const useFormResult = useForm({
    resolver: yupResolver(getSchema()),
  });

  const prepareForm = async () => {
    try {
      const createdByValuesResponse = await axios.get('/api/stravaRuns/createdByValues');
      setCreatedByValues(createdByValuesResponse.data);
      const data = (await axios.get('/api/stravaRuns/' + currentRunNumber)).data;
      useFormResult.reset(data);
    } catch (error: any) {
      handleServerError(error, navigate);
    }
  };

  useEffect(() => {
    prepareForm();
  }, []);

  const updateStravaRun = async (data: StravaRunDTO) => {
    window.scrollTo(0, 0);
    try {
      await axios.put('/api/stravaRuns/' + currentRunNumber, data);
      navigate('/stravaRuns', {
            state: {
              msgSuccess: t('stravaRun.update.success')
            }
          });
    } catch (error: any) {
      handleServerError(error, navigate, useFormResult.setError, t);
    }
  };

  return (<>
    <div className="flex flex-wrap mb-6">
      <h1 className="grow text-3xl md:text-4xl font-medium mb-2">{t('stravaRun.edit.headline')}</h1>
      <div>
        <Link to="/stravaRuns" className="inline-block text-white bg-gray-500 hover:bg-gray-600 focus:ring-gray-200 focus:ring-4 rounded px-5 py-2">{t('stravaRun.edit.back')}</Link>
      </div>
    </div>
    <form onSubmit={useFormResult.handleSubmit(updateStravaRun)} noValidate>
      <InputRow useFormResult={useFormResult} object="stravaRun" field="runNumber" disabled={true} type="number" />
      <InputRow useFormResult={useFormResult} object="stravaRun" field="customerId" required={true} type="number" />
      <InputRow useFormResult={useFormResult} object="stravaRun" field="runName" required={true} />
      <InputRow useFormResult={useFormResult} object="stravaRun" field="runDate" required={true} type="datepicker" />
      <InputRow useFormResult={useFormResult} object="stravaRun" field="miles" required={true} type="number" />
      <InputRow useFormResult={useFormResult} object="stravaRun" field="startLocation" required={true} type="number" />
      <InputRow useFormResult={useFormResult} object="stravaRun" field="createdAt" required={true} type="datepicker" />
      <InputRow useFormResult={useFormResult} object="stravaRun" field="updatedAt" type="datepicker" />
      <InputRow useFormResult={useFormResult} object="stravaRun" field="updatedBy" />
      <InputRow useFormResult={useFormResult} object="stravaRun" field="createdBy" required={true} type="select" options={createdByValues} />
      <input type="submit" value={t('stravaRun.edit.headline')} className="inline-block text-white bg-blue-600 hover:bg-blue-700 focus:ring-blue-300  focus:ring-4 rounded px-5 py-2 cursor-pointer mt-6" />
    </form>
  </>);
}
