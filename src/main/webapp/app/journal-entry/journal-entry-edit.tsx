import React, {useEffect} from 'react';
import {useTranslation} from 'react-i18next';
import {Link, useNavigate, useParams} from 'react-router';
import {handleServerError, setYupDefaults} from 'app/common/utils';
import {useForm} from 'react-hook-form';
import {yupResolver} from '@hookform/resolvers/yup';
import {JournalEntryDTO} from 'app/journal-entry/journal-entry-model';
import axios from 'axios';
import InputRow from 'app/common/input-row/input-row';
import useDocumentTitle from 'app/common/use-document-title';
import * as yup from 'yup';


function getSchema() {
    setYupDefaults();
    return yup.object({
        activityId: yup.string().emptyToNull(),
        entryDate: yup.string().emptyToNull().required(),
        perceivedEffort: yup.number().integer().emptyToNull()
            .min(1, 'Perceived effort must be between 1 and 10')
            .max(10, 'Perceived effort must be between 1 and 10'),
        feel: yup.string().emptyToNull(),
        bodyNotes: yup.string().emptyToNull().max(500),
        contextNotes: yup.string().emptyToNull().max(500),
        narrative: yup.string().emptyToNull(),
    });
}

const feelOptions = new Map([
    ['GREAT', 'Great'],
    ['GOOD', 'Good'],
    ['OK', 'OK'],
    ['ROUGH', 'Rough'],
    ['BAD', 'Bad'],
]);

export default function JournalEntryEdit() {
    const {t} = useTranslation();
    useDocumentTitle(t('journalEntry.edit.headline'));

    const navigate = useNavigate();
    const params = useParams();
    const currentId = +params.id!;

    const useFormResult = useForm({
        resolver: yupResolver(getSchema()),
    });

    const prepareForm = async () => {
        try {
            const data = (await axios.get('/api/journal/' + currentId)).data;
            useFormResult.reset(data);
        } catch (error: any) {
            handleServerError(error, navigate);
        }
    };

    useEffect(() => {
        prepareForm();
    }, []);

    const updateJournalEntry = async (data: JournalEntryDTO) => {
        window.scrollTo(0, 0);
        try {
            await axios.put('/api/journal/' + currentId, data);
            navigate('/journalEntries', {
                state: {
                    msgSuccess: t('journalEntry.update.success')
                }
            });
        } catch (error: any) {
            handleServerError(error, navigate, useFormResult.setError, t);
        }
    };

    return (<>
        <div className="flex flex-wrap mb-6">
            <h1 className="grow text-3xl md:text-4xl font-medium mb-2">{t('journalEntry.edit.headline')}</h1>
            <div>
                <Link to="/journalEntries"
                      className="inline-block text-white bg-gray-500 hover:bg-gray-600 focus:ring-gray-200 focus:ring-4 rounded px-5 py-2">{t('journalEntry.edit.back')}</Link>
            </div>
        </div>
        <form onSubmit={useFormResult.handleSubmit(updateJournalEntry)} noValidate>
            <input type="submit" value={t('journalEntry.edit.headline')}
                   className="inline-block text-white bg-blue-600 hover:bg-blue-700 focus:ring-blue-300 focus:ring-4 rounded px-5 py-2 cursor-pointer mt-6 mb-5"/>
            <InputRow useFormResult={useFormResult} object="journalEntry" field="id" disabled={true} type="number"/>
            <InputRow useFormResult={useFormResult} object="journalEntry" field="entryDate" required={true}
                      type="datepicker"/>
            <InputRow useFormResult={useFormResult} object="journalEntry" field="feel" type="select"
                      options={feelOptions}/>
            <InputRow useFormResult={useFormResult} object="journalEntry" field="perceivedEffort" type="number"
                      inputClass="w-full xl:w-1/4"/>
            <InputRow useFormResult={useFormResult} object="journalEntry" field="activityId"/>
            <InputRow useFormResult={useFormResult} object="journalEntry" field="bodyNotes" type="textarea"/>
            <InputRow useFormResult={useFormResult} object="journalEntry" field="contextNotes" type="textarea"/>
            <InputRow useFormResult={useFormResult} object="journalEntry" field="narrative" type="textarea"/>
            <input type="submit" value={t('journalEntry.edit.headline')}
                   className="inline-block text-white bg-blue-600 hover:bg-blue-700 focus:ring-blue-300 focus:ring-4 rounded px-5 py-2 cursor-pointer mt-6"/>
        </form>
    </>);
}
