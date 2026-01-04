import React, { useEffect, useState } from 'react';
import { useTranslation } from 'react-i18next';
import { Link, useNavigate, useParams } from 'react-router';
import { handleServerError, setYupDefaults } from 'app/common/utils';
import { useForm } from 'react-hook-form';
import { yupResolver } from '@hookform/resolvers/yup';
import { FileNameTrackerDTO } from 'app/file-name-tracker/file-name-tracker-model';
import axios from 'axios';
import InputRow from 'app/common/input-row/input-row';
import useDocumentTitle from 'app/common/use-document-title';
import * as yup from 'yup';


function getSchema() {
  setYupDefaults();
  return yup.object({
    fileName: yup.string().emptyToNull().required(),
    createdAt: yup.string().emptyToNull().offsetDateTime().required(),
    updatedAt: yup.string().emptyToNull().offsetDateTime(),
    updatedBy: yup.string().emptyToNull().max(40),
    createdBy: yup.number().integer().emptyToNull().required()
  });
}

export default function FileNameTrackerEdit() {
  const { t } = useTranslation();
  useDocumentTitle(t('fileNameTracker.edit.headline'));

  const navigate = useNavigate();
  const [createdByValues, setCreatedByValues] = useState<Map<number,string>>(new Map());
  const params = useParams();
  const currentId = +params.id!;

  const useFormResult = useForm({
    resolver: yupResolver(getSchema()),
  });

  const prepareForm = async () => {
    try {
      const createdByValuesResponse = await axios.get('/api/fileNameTrackers/createdByValues');
      setCreatedByValues(createdByValuesResponse.data);
      const data = (await axios.get('/api/fileNameTrackers/' + currentId)).data;
      useFormResult.reset(data);
    } catch (error: any) {
      handleServerError(error, navigate);
    }
  };

  useEffect(() => {
    prepareForm();
  }, []);

  const updateFileNameTracker = async (data: FileNameTrackerDTO) => {
    window.scrollTo(0, 0);
    try {
      await axios.put('/api/fileNameTrackers/' + currentId, data);
      navigate('/fileNameTrackers', {
            state: {
              msgSuccess: t('fileNameTracker.update.success')
            }
          });
    } catch (error: any) {
      handleServerError(error, navigate, useFormResult.setError, t);
    }
  };

  return (<>
    <div className="flex flex-wrap mb-6">
      <h1 className="grow text-3xl md:text-4xl font-medium mb-2">{t('fileNameTracker.edit.headline')}</h1>
      <div>
        <Link to="/fileNameTrackers" className="inline-block text-white bg-gray-500 hover:bg-gray-600 focus:ring-gray-200 focus:ring-4 rounded px-5 py-2">{t('fileNameTracker.edit.back')}</Link>
      </div>
    </div>
    <form onSubmit={useFormResult.handleSubmit(updateFileNameTracker)} noValidate>
      <InputRow useFormResult={useFormResult} object="fileNameTracker" field="id" disabled={true} type="number" />
      <InputRow useFormResult={useFormResult} object="fileNameTracker" field="fileName" required={true} type="textarea" />
      <InputRow useFormResult={useFormResult} object="fileNameTracker" field="createdAt" required={true} />
      <InputRow useFormResult={useFormResult} object="fileNameTracker" field="updatedAt" />
      <InputRow useFormResult={useFormResult} object="fileNameTracker" field="updatedBy" />
      <InputRow useFormResult={useFormResult} object="fileNameTracker" field="createdBy" required={true} type="select" options={createdByValues} />
      <input type="submit" value={t('fileNameTracker.edit.headline')} className="inline-block text-white bg-blue-600 hover:bg-blue-700 focus:ring-blue-300  focus:ring-4 rounded px-5 py-2 cursor-pointer mt-6" />
    </form>
  </>);
}
