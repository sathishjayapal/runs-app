import React, { useEffect, useState } from 'react';
import { useTranslation } from 'react-i18next';
import { UseFormReturn } from 'react-hook-form';
import InputErrors from 'app/common/input-row/input-errors';


export default function ElapsedTimeInput({ useFormResult, object, field, required = false, disabled = false }: ElapsedTimeInputParams) {
  const { t } = useTranslation();
  const label = t(object + '.' + field + '.label') + (required ? '*' : '');

  const { setValue, watch, formState: { errors } } = useFormResult;
  const [hours, setHours] = useState('0');
  const [minutes, setMinutes] = useState('0');
  const [seconds, setSeconds] = useState('0');

  const elapsedTimeValue = watch(field);

  useEffect(() => {
    if (elapsedTimeValue && typeof elapsedTimeValue === 'string') {
      const parts = elapsedTimeValue.split(':');
      if (parts.length === 3) {
        setHours(parts[0] || '0');
        setMinutes(parts[1] || '0');
        setSeconds(parts[2] || '0');
      }
    }
  }, [elapsedTimeValue]);

  const updateElapsedTime = (h: string, m: string, s: string) => {
    const paddedHours = h.padStart(2, '0');
    const paddedMinutes = m.padStart(2, '0');
    const paddedSeconds = s.padStart(2, '0');
    const timeString = `${paddedHours}:${paddedMinutes}:${paddedSeconds}`;
    setValue(field, timeString, { shouldValidate: true });
  };

  const handleHoursChange = (e: React.ChangeEvent<HTMLInputElement>) => {
    const value = e.target.value.replace(/\D/g, '');
    const numValue = Math.min(Math.max(0, parseInt(value || '0')), 99);
    const strValue = numValue.toString();
    setHours(strValue);
    updateElapsedTime(strValue, minutes, seconds);
  };

  const handleMinutesChange = (e: React.ChangeEvent<HTMLInputElement>) => {
    const value = e.target.value.replace(/\D/g, '');
    const numValue = Math.min(Math.max(0, parseInt(value || '0')), 59);
    const strValue = numValue.toString();
    setMinutes(strValue);
    updateElapsedTime(hours, strValue, seconds);
  };

  const handleSecondsChange = (e: React.ChangeEvent<HTMLInputElement>) => {
    const value = e.target.value.replace(/\D/g, '');
    const numValue = Math.min(Math.max(0, parseInt(value || '0')), 59);
    const strValue = numValue.toString();
    setSeconds(strValue);
    updateElapsedTime(hours, minutes, strValue);
  };

  const getInputClasses = () => {
    return (errors[field] ? 'border-red-600 ' : '') + (disabled ? 'bg-gray-100 ' : '');
  };

  return (
    <div className="md:grid grid-cols-12 gap-4 mb-4">
      <label className="col-span-2 py-2">
        {label}
      </label>
      <div className="col-span-10">
        <div className="flex gap-2 items-center">
          <div className="flex flex-col">
            <input
              type="number"
              value={hours}
              onChange={handleHoursChange}
              disabled={disabled}
              min="0"
              max="99"
              className={'w-20 border-gray-300 rounded ' + getInputClasses()}
              placeholder="HH"
            />
            <span className="text-xs text-gray-500 mt-1">{t('elapsedTime.hours')}</span>
          </div>
          <span className="text-xl">:</span>
          <div className="flex flex-col">
            <input
              type="number"
              value={minutes}
              onChange={handleMinutesChange}
              disabled={disabled}
              min="0"
              max="59"
              className={'w-20 border-gray-300 rounded ' + getInputClasses()}
              placeholder="MM"
            />
            <span className="text-xs text-gray-500 mt-1">{t('elapsedTime.minutes')}</span>
          </div>
          <span className="text-xl">:</span>
          <div className="flex flex-col">
            <input
              type="number"
              value={seconds}
              onChange={handleSecondsChange}
              disabled={disabled}
              min="0"
              max="59"
              className={'w-20 border-gray-300 rounded ' + getInputClasses()}
              placeholder="SS"
            />
            <span className="text-xs text-gray-500 mt-1">{t('elapsedTime.seconds')}</span>
          </div>
        </div>
        <InputErrors errors={errors} field={field} />
      </div>
    </div>
  );
}

interface ElapsedTimeInputParams {
  useFormResult: UseFormReturn<any, any, any|undefined>;
  object: string;
  field: string;
  required?: boolean;
  disabled?: boolean;
}
