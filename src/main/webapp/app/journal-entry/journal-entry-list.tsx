import React, {useEffect, useState} from 'react';
import {useTranslation} from 'react-i18next';
import {Link, useNavigate} from 'react-router';
import {handleServerError} from 'app/common/utils';
import {JournalEntryDTO} from 'app/journal-entry/journal-entry-model';
import axios from 'axios';
import useDocumentTitle from 'app/common/use-document-title';


export default function JournalEntryList() {
    const {t} = useTranslation();
    useDocumentTitle(t('journalEntry.list.headline'));

    const [journalEntries, setJournalEntries] = useState<JournalEntryDTO[]>([]);
    const navigate = useNavigate();

    const getAllJournalEntries = async () => {
        try {
            const response = await axios.get('/api/journal/recent');
            setJournalEntries(response.data);
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
            await axios.delete('/api/journal/' + id);
            getAllJournalEntries();
        } catch (error: any) {
            handleServerError(error, navigate);
        }
    };

    useEffect(() => {
        getAllJournalEntries();
    }, []);

    const feelBadgeClass = (feel: string | null | undefined) => {
        switch (feel) {
            case 'GREAT':
                return 'bg-green-100 text-green-800';
            case 'GOOD':
                return 'bg-blue-100 text-blue-800';
            case 'OK':
                return 'bg-gray-100 text-gray-800';
            case 'ROUGH':
                return 'bg-yellow-100 text-yellow-800';
            case 'BAD':
                return 'bg-red-100 text-red-800';
            default:
                return 'bg-gray-100 text-gray-500';
        }
    };

    return (<>
        <div className="flex flex-wrap mb-6">
            <h1 className="grow text-3xl md:text-4xl font-medium mb-2">{t('journalEntry.list.headline')}</h1>
            <div>
                <Link to="/journalEntries/add"
                      className="inline-block text-white bg-blue-600 hover:bg-blue-700 focus:ring-blue-300 focus:ring-4 rounded px-5 py-2">{t('journalEntry.list.createNew')}</Link>
            </div>
        </div>
        {journalEntries.length === 0 ? (
            <div>{t('journalEntry.list.empty')}</div>
        ) : (
            <div className="overflow-x-auto">
                <table className="w-full border-collapse">
                    <thead>
                    <tr className="bg-gray-100">
                        <th className="border border-gray-300 px-4 py-2 text-left">{t('journalEntry.entryDate.label')}</th>
                        <th className="border border-gray-300 px-4 py-2 text-center">{t('journalEntry.feel.label')}</th>
                        <th className="border border-gray-300 px-4 py-2 text-center">{t('journalEntry.perceivedEffort.label')}</th>
                        <th className="border border-gray-300 px-4 py-2 text-left">{t('journalEntry.activityId.label')}</th>
                        <th className="border border-gray-300 px-4 py-2 text-left">{t('journalEntry.bodyNotes.label')}</th>
                        <th className="border border-gray-300 px-4 py-2 text-center">{t('journalEntry.list.aiStatus')}</th>
                        <th className="border border-gray-300 px-4 py-2 text-center">{t('journalEntry.list.actions')}</th>
                    </tr>
                    </thead>
                    <tbody>
                    {journalEntries.map((entry) => (
                        <tr key={entry.id} className="hover:bg-gray-50">
                            <td className="border border-gray-300 px-4 py-2 font-medium">
                                <Link to={'/journalEntries/edit/' + entry.id} className="text-blue-600 hover:underline">
                                    {entry.entryDate ? new Date(entry.entryDate).toLocaleDateString() : '-'}
                                </Link>
                            </td>
                            <td className="border border-gray-300 px-4 py-2 text-center">
                                {entry.feel ? (
                                    <span
                                        className={'text-xs font-semibold px-2 py-1 rounded-full ' + feelBadgeClass(entry.feel)}>
                  {entry.feel}
                </span>
                                ) : '-'}
                            </td>
                            <td className="border border-gray-300 px-4 py-2 text-center font-semibold">
                                {entry.perceivedEffort != null ? entry.perceivedEffort + '/10' : '-'}
                            </td>
                            <td className="border border-gray-300 px-4 py-2 font-mono text-sm">
                                {entry.activityId || '-'}
                            </td>
                            <td className="border border-gray-300 px-4 py-2 text-sm text-gray-600 max-w-xs truncate">
                                {entry.bodyNotes || entry.narrative || '-'}
                            </td>
                            <td className="border border-gray-300 px-4 py-2 text-center">
              <span
                  className={'text-xs font-semibold px-2 py-1 rounded-full ' + (entry.embedded ? 'bg-green-100 text-green-800' : 'bg-yellow-100 text-yellow-800')}>
                {entry.embedded ? t('journalEntry.list.indexed') : t('journalEntry.list.pending')}
              </span>
                            </td>
                            <td className="border border-gray-300 px-4 py-2 text-center">
                                <div className="flex gap-2 justify-center">
                                    <Link to={'/journalEntries/edit/' + entry.id}
                                          className="inline-flex items-center text-white bg-blue-600 hover:bg-blue-700 focus:ring-4 focus:ring-blue-300 rounded px-3 py-1.5 text-sm font-medium transition-colors">
                                        <svg className="w-4 h-4 mr-1" fill="none" stroke="currentColor"
                                             viewBox="0 0 24 24">
                                            <path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2"
                                                  d="M11 5H6a2 2 0 00-2 2v11a2 2 0 002 2h11a2 2 0 002-2v-5m-1.414-9.414a2 2 0 112.828 2.828L11.828 15H9v-2.828l8.586-8.586z"/>
                                        </svg>
                                        {t('journalEntry.list.edit')}
                                    </Link>
                                    <button type="button" onClick={() => confirmDelete(entry.id!)}
                                            className="inline-flex items-center text-white bg-red-600 hover:bg-red-700 focus:ring-4 focus:ring-red-300 rounded px-3 py-1.5 text-sm font-medium cursor-pointer transition-colors">
                                        <svg className="w-4 h-4 mr-1" fill="none" stroke="currentColor"
                                             viewBox="0 0 24 24">
                                            <path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2"
                                                  d="M19 7l-.867 12.142A2 2 0 0116.138 21H7.862a2 2 0 01-1.995-1.858L5 7m5 4v6m4-6v6m1-10V4a1 1 0 00-1-1h-4a1 1 0 00-1 1v3M4 7h16"/>
                                        </svg>
                                        {t('journalEntry.list.delete')}
                                    </button>
                                </div>
                            </td>
                        </tr>
                    ))}
                    </tbody>
                </table>
            </div>
        )}
    </>);
}