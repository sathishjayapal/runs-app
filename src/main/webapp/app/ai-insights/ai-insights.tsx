import React, {useEffect, useRef, useState} from 'react';
import {useTranslation} from 'react-i18next';
import {useNavigate} from 'react-router';
import {handleServerError} from 'app/common/utils';
import axios from 'axios';
import useDocumentTitle from 'app/common/use-document-title';


interface RagAnswer {
    answer?: string;
    query?: string;
    sources?: string[];
    error?: string;
}

interface RecentAnalysis {
    id?: string | number;
    content?: string;
    metadata?: Record<string, unknown>;
    createdAt?: string;
}

interface AnalysisJob {
    jobId: string;
    status: 'PENDING' | 'PROCESSING' | 'DONE' | 'FAILED';
    createdAt?: string;
    completedAt?: string;
    errorMessage?: string;
}

interface AnalysisResult {
    summary?: string;
    insights?: { category: string; observation: string; recommendation: string }[];
    recommendations?: string[];
    riskFlags?: string[];
    confidenceScore?: number;
    metrics?: {
        totalRuns: number;
        totalDistanceKm: number;
        totalDuration: string;
        averagePaceMinPerKm?: number;
        averageHeartRate?: number;
        totalCalories?: number;
    };
    cachedResult?: boolean;
}

const SUGGESTED_QUERIES = [
    'Based on my recent runs and journal entries, what should I focus on next week? Any injury risks I should watch?',
    'What patterns do you see in my recovery and fatigue? When do I perform best?',
    'How is my training load trending? Am I overtraining or undertraining?',
    'What does my heart rate data suggest about my aerobic fitness?',
];

const POLL_INTERVAL_MS = 5000;

export default function AiInsights() {
    const {t} = useTranslation();
    useDocumentTitle(t('aiInsights.headline'));

    const [query, setQuery] = useState('');
    const [answer, setAnswer] = useState<RagAnswer | null>(null);
    const [recentAnalyses, setRecentAnalyses] = useState<RecentAnalysis[]>([]);
    const [searching, setSearching] = useState(false);
    const [loadingRecent, setLoadingRecent] = useState(true);
    const navigate = useNavigate();
    const answerRef = useRef<HTMLDivElement>(null);

    const [analysisJob, setAnalysisJob] = useState<AnalysisJob | null>(null);
    const [analysisResult, setAnalysisResult] = useState<AnalysisResult | null>(null);
    const [submittingAnalysis, setSubmittingAnalysis] = useState(false);
    const pollTimerRef = useRef<ReturnType<typeof setTimeout> | null>(null);
    const resultRef = useRef<HTMLDivElement>(null);

    const loadRecentAnalyses = async () => {
        setLoadingRecent(true);
        try {
            const response = await axios.get('/api/rag/recent');
            setRecentAnalyses(Array.isArray(response.data) ? response.data : []);
        } catch (error: any) {
            if (error?.response?.status === 401) {
                window.location.href = '/login';
                return;
            }
            // recent analyses are optional — don't block the page on failure
            setRecentAnalyses([]);
        } finally {
            setLoadingRecent(false);
        }
    };

    useEffect(() => {
        loadRecentAnalyses();
    }, []);

    const handleSearch = async (e?: React.FormEvent) => {
        if (e) e.preventDefault();
        const trimmed = query.trim();
        if (!trimmed) return;
        setSearching(true);
        setAnswer(null);
        try {
            const response = await axios.post('/api/rag/search', {query: trimmed});
            setAnswer(response.data);
            setTimeout(() => answerRef.current?.scrollIntoView({behavior: 'smooth', block: 'start'}), 100);
        } catch (error: any) {
            if (error?.response?.status === 401) {
                window.location.href = '/login';
                return;
            }
            handleServerError(error, navigate);
        } finally {
            setSearching(false);
        }
    };

    const handleSuggestedQuery = (suggested: string) => {
        setQuery(suggested);
    };

    const stopPolling = () => {
        if (pollTimerRef.current) {
            clearTimeout(pollTimerRef.current);
            pollTimerRef.current = null;
        }
    };

    useEffect(() => () => stopPolling(), []);

    const pollJobStatus = (jobId: string) => {
        pollTimerRef.current = setTimeout(async () => {
            try {
                const statusRes = await axios.get(`/api/rag/analyze/status/${jobId}`);
                const job: AnalysisJob = statusRes.data;
                setAnalysisJob(job);

                if (job.status === 'DONE') {
                    const resultRes = await axios.get(`/api/rag/analyze/result/${jobId}`);
                    setAnalysisResult(resultRes.data);
                    setSubmittingAnalysis(false);
                    loadRecentAnalyses();
                    setTimeout(() => resultRef.current?.scrollIntoView({behavior: 'smooth', block: 'start'}), 100);
                } else if (job.status === 'FAILED') {
                    setSubmittingAnalysis(false);
                } else {
                    pollJobStatus(jobId);
                }
            } catch (error: any) {
                setSubmittingAnalysis(false);
                if (error?.response?.status === 401) {
                    window.location.href = '/login';
                }
            }
        }, POLL_INTERVAL_MS);
    };

    const handleAnalyzeRuns = async () => {
        setSubmittingAnalysis(true);
        setAnalysisJob(null);
        setAnalysisResult(null);
        stopPolling();
        try {
            const runsRes = await axios.get('/api/garminRuns?size=20&sort=activityDate,desc');
            const runs = (runsRes.data?.content ?? runsRes.data ?? []).slice(0, 10);
            if (runs.length === 0) {
                setSubmittingAnalysis(false);
                return;
            }
            const jobRes = await axios.post('/api/rag/analyze', {runs, forceRefresh: false});
            const job: AnalysisJob = jobRes.data;
            setAnalysisJob(job);
            pollJobStatus(job.jobId);
        } catch (error: any) {
            setSubmittingAnalysis(false);
            if (error?.response?.status === 401) {
                window.location.href = '/login';
                return;
            }
            handleServerError(error, navigate);
        }
    };

    const renderAnswer = (text: string) => {
        // Render newlines as paragraphs and bold **text**
        return text
            .split('\n\n')
            .filter(Boolean)
            .map((paragraph, i) => {
                const withBold = paragraph.replace(/\*\*(.*?)\*\*/g, '<strong>$1</strong>');
                const withNewlines = withBold.replace(/\n/g, '<br/>');
                return (
                    <p key={i} className="mb-3 leading-relaxed"
                       dangerouslySetInnerHTML={{__html: withNewlines}}/>
                );
            });
    };

    return (
        <>
            <div className="flex flex-wrap mb-6">
                <div className="grow">
                    <h1 className="text-3xl md:text-4xl font-medium mb-1">{t('aiInsights.headline')}</h1>
                    <p className="text-gray-500 text-sm">{t('aiInsights.subtitle')}</p>
                </div>
            </div>

            {/* Query form */}
            <form onSubmit={handleSearch} className="mb-8">
                <div className="bg-white border border-gray-200 rounded-lg shadow-sm p-5">
                    <label htmlFor="ragQuery" className="block text-sm font-medium text-gray-700 mb-2">
                        {t('aiInsights.queryLabel')}
                    </label>
                    <textarea
                        id="ragQuery"
                        rows={3}
                        value={query}
                        onChange={(e) => setQuery(e.target.value)}
                        placeholder={t('aiInsights.queryPlaceholder')}
                        className="w-full border border-gray-300 rounded px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-blue-400 resize-none"
                        onKeyDown={(e) => {
                            if (e.key === 'Enter' && (e.metaKey || e.ctrlKey)) {
                                handleSearch();
                            }
                        }}
                    />
                    <div className="flex items-center justify-between mt-3">
                        <span className="text-xs text-gray-400">{t('aiInsights.queryHint')}</span>
                        <button
                            type="submit"
                            disabled={searching || !query.trim()}
                            className="inline-flex items-center gap-2 text-white bg-blue-600 hover:bg-blue-700 disabled:opacity-50 disabled:cursor-not-allowed focus:ring-4 focus:ring-blue-300 rounded px-5 py-2 text-sm font-medium transition-colors"
                        >
                            {searching ? (
                                <>
                                    <svg className="animate-spin h-4 w-4" fill="none" viewBox="0 0 24 24">
                                        <circle className="opacity-25" cx="12" cy="12" r="10" stroke="currentColor"
                                                strokeWidth="4"/>
                                        <path className="opacity-75" fill="currentColor"
                                              d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4z"/>
                                    </svg>
                                    {t('aiInsights.searching')}
                                </>
                            ) : (
                                <>
                                    <svg className="h-4 w-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                                        <path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2"
                                              d="M9.663 17h4.673M12 3v1m6.364 1.636l-.707.707M21 12h-1M4 12H3m3.343-5.657l-.707-.707m2.828 9.9a5 5 0 117.072 0l-.346.09A4.923 4.923 0 0112 17a4.923 4.923 0 01-1.997-.42l-.345-.09z"/>
                                    </svg>
                                    {t('aiInsights.askAi')}
                                </>
                            )}
                        </button>
                    </div>
                </div>
            </form>

            {/* Suggested queries */}
            <div className="mb-8">
                <h2 className="text-sm font-semibold text-gray-500 uppercase tracking-wide mb-3">
                    {t('aiInsights.suggestedQueries')}
                </h2>
                <div className="flex flex-wrap gap-2">
                    {SUGGESTED_QUERIES.map((sq, idx) => (
                        <button
                            key={idx}
                            type="button"
                            onClick={() => handleSuggestedQuery(sq)}
                            className="text-xs bg-gray-100 hover:bg-blue-50 hover:text-blue-700 border border-gray-200 hover:border-blue-300 text-gray-600 rounded-full px-3 py-1.5 cursor-pointer transition-colors text-left"
                        >
                            {sq.length > 80 ? sq.slice(0, 80) + '…' : sq}
                        </button>
                    ))}
                </div>
            </div>

            {/* AI Answer */}
            {answer && (
                <div ref={answerRef} className="mb-8">
                    <div
                        className="bg-gradient-to-br from-blue-50 to-indigo-50 border border-blue-200 rounded-lg p-6 shadow-sm">
                        <div className="flex items-center gap-2 mb-4">
                            <svg className="h-5 w-5 text-blue-600" fill="none" stroke="currentColor"
                                 viewBox="0 0 24 24">
                                <path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2"
                                      d="M9.663 17h4.673M12 3v1m6.364 1.636l-.707.707M21 12h-1M4 12H3m3.343-5.657l-.707-.707m2.828 9.9a5 5 0 117.072 0l-.346.09A4.923 4.923 0 0112 17a4.923 4.923 0 01-1.997-.42l-.345-.09z"/>
                            </svg>
                            <h2 className="font-semibold text-blue-900">{t('aiInsights.aiResponse')}</h2>
                        </div>
                        {answer.error ? (
                            <p className="text-red-600 text-sm">{answer.error}</p>
                        ) : (
                            <div className="text-gray-800 text-sm">
                                {answer.answer ? renderAnswer(answer.answer) : (
                                    <pre className="whitespace-pre-wrap text-xs font-mono bg-white/60 rounded p-3">
                                        {JSON.stringify(answer, null, 2)}
                                    </pre>
                                )}
                            </div>
                        )}
                        {answer.sources && answer.sources.length > 0 && (
                            <div className="mt-4 pt-3 border-t border-blue-200">
                                <p className="text-xs text-blue-700 font-medium mb-1">{t('aiInsights.sources')}</p>
                                <ul className="list-disc list-inside text-xs text-blue-600 space-y-0.5">
                                    {answer.sources.map((src, i) => (
                                        <li key={i}>{src}</li>
                                    ))}
                                </ul>
                            </div>
                        )}
                        {answer.query && (
                            <p className="mt-3 text-xs text-gray-400 italic">
                                {t('aiInsights.queryWas')}: &ldquo;{answer.query}&rdquo;
                            </p>
                        )}
                    </div>
                </div>
            )}

            {/* Analyze My Runs */}
            <div className="mb-8">
                <div className="flex items-center justify-between mb-3">
                    <h2 className="text-xl font-semibold">Analyze My Runs</h2>
                    <button
                        type="button"
                        onClick={handleAnalyzeRuns}
                        disabled={submittingAnalysis}
                        className="inline-flex items-center gap-2 text-white bg-green-600 hover:bg-green-700 disabled:opacity-50 disabled:cursor-not-allowed focus:ring-4 focus:ring-green-300 rounded px-5 py-2 text-sm font-medium transition-colors"
                    >
                        {submittingAnalysis ? (
                            <>
                                <svg className="animate-spin h-4 w-4" fill="none" viewBox="0 0 24 24">
                                    <circle className="opacity-25" cx="12" cy="12" r="10" stroke="currentColor"
                                            strokeWidth="4"/>
                                    <path className="opacity-75" fill="currentColor"
                                          d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4z"/>
                                </svg>
                                {analysisJob?.status === 'PROCESSING' ? 'AI is analyzing…' : 'Starting…'}
                            </>
                        ) : (
                            <>
                                <svg className="h-4 w-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                                    <path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2"
                                          d="M13 10V3L4 14h7v7l9-11h-7z"/>
                                </svg>
                                Analyze My Recent Runs
                            </>
                        )}
                    </button>
                </div>
                <p className="text-sm text-gray-500 mb-4">Fetches your 10 most recent Garmin runs and generates an AI
                    coaching analysis. Results appear below when ready (may take 1-3 minutes).</p>

                {/* Job status banner */}
                {analysisJob && submittingAnalysis && (
                    <div
                        className="flex items-center gap-3 bg-blue-50 border border-blue-200 rounded-lg px-4 py-3 text-sm text-blue-800">
                        <svg className="animate-spin h-4 w-4 shrink-0" fill="none" viewBox="0 0 24 24">
                            <circle className="opacity-25" cx="12" cy="12" r="10" stroke="currentColor"
                                    strokeWidth="4"/>
                            <path className="opacity-75" fill="currentColor"
                                  d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4z"/>
                        </svg>
                        <span>Status: <strong>{analysisJob.status}</strong> — checking every 5 seconds…</span>
                    </div>
                )}

                {analysisJob?.status === 'FAILED' && (
                    <div className="bg-red-50 border border-red-200 rounded-lg px-4 py-3 text-sm text-red-700">
                        Analysis failed: {analysisJob.errorMessage ?? 'Unknown error'}
                    </div>
                )}

                {/* Analysis result */}
                {analysisResult && (
                    <div ref={resultRef}
                         className="bg-gradient-to-br from-green-50 to-emerald-50 border border-green-200 rounded-lg p-6 shadow-sm mt-4">
                        <div className="flex items-center justify-between mb-4">
                            <h3 className="font-semibold text-green-900 flex items-center gap-2">
                                <svg className="h-5 w-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                                    <path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2"
                                          d="M9 12l2 2 4-4m6 2a9 9 0 11-18 0 9 9 0 0118 0z"/>
                                </svg>
                                AI Analysis Complete
                            </h3>
                            {analysisResult.cachedResult && (
                                <span
                                    className="text-xs bg-yellow-100 text-yellow-700 border border-yellow-200 rounded-full px-2 py-0.5">Cached</span>
                            )}
                        </div>

                        {analysisResult.metrics && (
                            <div className="grid grid-cols-2 sm:grid-cols-3 gap-3 mb-4">
                                {[
                                    {label: 'Runs', value: analysisResult.metrics.totalRuns},
                                    {label: 'Distance', value: `${analysisResult.metrics.totalDistanceKm} km`},
                                    {label: 'Duration', value: analysisResult.metrics.totalDuration},
                                    analysisResult.metrics.averagePaceMinPerKm ? {
                                        label: 'Avg Pace',
                                        value: `${analysisResult.metrics.averagePaceMinPerKm} min/km`
                                    } : null,
                                    analysisResult.metrics.averageHeartRate ? {
                                        label: 'Avg HR',
                                        value: `${analysisResult.metrics.averageHeartRate} bpm`
                                    } : null,
                                    analysisResult.metrics.totalCalories ? {
                                        label: 'Calories',
                                        value: analysisResult.metrics.totalCalories
                                    } : null,
                                ].filter(Boolean).map((m, i) => (
                                    <div key={i} className="bg-white/70 rounded-lg px-3 py-2 text-center">
                                        <p className="text-xs text-gray-500">{m!.label}</p>
                                        <p className="font-semibold text-gray-800 text-sm">{m!.value}</p>
                                    </div>
                                ))}
                            </div>
                        )}

                        {analysisResult.summary && (
                            <p className="text-gray-800 text-sm mb-4 leading-relaxed">{analysisResult.summary}</p>
                        )}

                        {analysisResult.insights && analysisResult.insights.length > 0 && (
                            <div className="mb-4">
                                <h4 className="text-sm font-semibold text-green-800 mb-2">Insights</h4>
                                <div className="space-y-2">
                                    {analysisResult.insights.map((insight, i) => (
                                        <div key={i} className="bg-white/60 rounded-lg px-3 py-2 text-sm">
                                            <span className="font-medium text-green-700">{insight.category}: </span>
                                            <span className="text-gray-700">{insight.observation}</span>
                                            {insight.recommendation && (
                                                <p className="text-gray-500 text-xs mt-1 italic">→ {insight.recommendation}</p>
                                            )}
                                        </div>
                                    ))}
                                </div>
                            </div>
                        )}

                        {analysisResult.recommendations && analysisResult.recommendations.length > 0 && (
                            <div className="mb-4">
                                <h4 className="text-sm font-semibold text-green-800 mb-2">Recommendations</h4>
                                <ul className="list-disc list-inside text-sm text-gray-700 space-y-1">
                                    {analysisResult.recommendations.map((r, i) => <li key={i}>{r}</li>)}
                                </ul>
                            </div>
                        )}

                        {analysisResult.riskFlags && analysisResult.riskFlags.length > 0 && (
                            <div>
                                <h4 className="text-sm font-semibold text-red-700 mb-2">Risk Flags</h4>
                                <ul className="list-disc list-inside text-sm text-red-600 space-y-1">
                                    {analysisResult.riskFlags.map((r, i) => <li key={i}>{r}</li>)}
                                </ul>
                            </div>
                        )}

                        {analysisResult.confidenceScore != null && (
                            <p className="text-xs text-gray-400 mt-4 text-right">Confidence: {analysisResult.confidenceScore}%</p>
                        )}
                    </div>
                )}
            </div>

            {/* Recent Analyses */}
            <div>
                <h2 className="text-xl font-semibold mb-3">{t('aiInsights.recentAnalyses')}</h2>
                {loadingRecent ? (
                    <div className="flex items-center gap-2 text-gray-400 text-sm py-4">
                        <svg className="animate-spin h-4 w-4" fill="none" viewBox="0 0 24 24">
                            <circle className="opacity-25" cx="12" cy="12" r="10" stroke="currentColor"
                                    strokeWidth="4"/>
                            <path className="opacity-75" fill="currentColor"
                                  d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4z"/>
                        </svg>
                        {t('aiInsights.loadingRecent')}
                    </div>
                ) : recentAnalyses.length === 0 ? (
                    <div className="bg-gray-50 border border-gray-200 rounded-lg p-6 text-center text-gray-500 text-sm">
                        <svg className="h-10 w-10 mx-auto mb-2 text-gray-300" fill="none" stroke="currentColor"
                             viewBox="0 0 24 24">
                            <path strokeLinecap="round" strokeLinejoin="round" strokeWidth="1.5"
                                  d="M9 5H7a2 2 0 00-2 2v12a2 2 0 002 2h10a2 2 0 002-2V7a2 2 0 00-2-2h-2M9 5a2 2 0 002 2h2a2 2 0 002-2M9 5a2 2 0 012-2h2a2 2 0 012 2"/>
                        </svg>
                        {t('aiInsights.noRecentAnalyses')}
                    </div>
                ) : (
                    <div className="space-y-3">
                        {recentAnalyses.map((analysis, idx) => (
                            <div key={analysis.id ?? idx}
                                 className="bg-white border border-gray-200 rounded-lg p-4 shadow-sm">
                                {analysis.content && (
                                    <p className="text-sm text-gray-700 leading-relaxed line-clamp-4">
                                        {analysis.content}
                                    </p>
                                )}
                                {!analysis.content && (
                                    <pre className="text-xs font-mono text-gray-600 whitespace-pre-wrap">
                                        {JSON.stringify(analysis, null, 2)}
                                    </pre>
                                )}
                                {analysis.createdAt && (
                                    <p className="text-xs text-gray-400 mt-2">
                                        {new Date(analysis.createdAt).toLocaleString()}
                                    </p>
                                )}
                            </div>
                        ))}
                    </div>
                )}
            </div>
        </>
    );
}
