import React, { useEffect, useState } from 'react';
import { useTranslation } from 'react-i18next';
import { Link, useNavigate } from 'react-router';
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

export default function FileNameTrackerAdd() {
  const { t } = useTranslation();
  useDocumentTitle(t('fileNameTracker.add.headline'));

  const navigate = useNavigate();
  const [createdByValues, setCreatedByValues] = useState<Map<number,string>>(new Map());

  const useFormResult = useForm({
    resolver: yupResolver(getSchema()),
  });

  const prepareRelations = async () => {
    try {
      const createdByValuesResponse = await axios.get('/api/fileNameTrackers/createdByValues');
      setCreatedByValues(createdByValuesResponse.data);
    } catch (error: any) {
      handleServerError(error, navigate);
    }
  };

  useEffect(() => {
    prepareRelations();
  }, []);

  const createFileNameTracker = async (data: FileNameTrackerDTO) => {
    window.scrollTo(0, 0);
    try {
      await axios.post('/api/fileNameTrackers', data);
      navigate('/fileNameTrackers', {
            state: {
              msgSuccess: t('fileNameTracker.create.success')
            }
          });
    } catch (error: any) {
      handleServerError(error, navigate, useFormResult.setError, t);
    }
  };

  return (<>
    <div className="flex flex-wrap mb-6">
      <h1 className="grow text-3xl md:text-4xl font-medium mb-2">{t('fileNameTracker.add.headline')}</h1>
      <div>
        <Link to="/fileNameTrackers" className="inline-block text-white bg-gray-500 hover:bg-gray-600 focus:ring-gray-200 focus:ring-4 rounded px-5 py-2">{t('fileNameTracker.add.back')}</Link>
      </div>
    </div>
    <form onSubmit={useFormResult.handleSubmit(createFileNameTracker)} noValidate>
      <InputRow useFormResult={useFormResult} object="fileNameTracker" field="fileName" required={true} type="textarea" />
      <InputRow useFormResult={useFormResult} object="fileNameTracker" field="createdAt" required={true} />
      <InputRow useFormResult={useFormResult} object="fileNameTracker" field="updatedAt" />
      <InputRow useFormResult={useFormResult} object="fileNameTracker" field="updatedBy" />
      <InputRow useFormResult={useFormResult} object="fileNameTracker" field="createdBy" required={true} type="select" options={createdByValues} />
      <input type="submit" value={t('fileNameTracker.add.headline')} className="inline-block text-white bg-blue-600 hover:bg-blue-700 focus:ring-blue-300  focus:ring-4 rounded px-5 py-2 cursor-pointer mt-6" />
    </form>
  </>);
}
