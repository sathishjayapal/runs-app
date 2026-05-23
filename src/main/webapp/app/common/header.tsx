import React, {useEffect, useRef} from 'react';
import {Link} from 'react-router';
import {useTranslation} from 'react-i18next';
import {useAuth} from 'app/common/use-auth';


export default function Header() {
  const { t } = useTranslation();
  const {currentUser, isAdmin, logout} = useAuth();
  const headerRef = useRef<HTMLElement|null>(null);

  const handleClick = (event: Event) => {
    // close any open dropdown
    const $clickedDropdown = (event.target as HTMLElement).closest('.js-dropdown');
    const $dropdowns = headerRef.current!.querySelectorAll('.js-dropdown');
    $dropdowns.forEach(($dropdown:Element) => {
      if ($clickedDropdown !== $dropdown && $dropdown.getAttribute('data-dropdown-keepopen') !== 'true') {
        $dropdown.ariaExpanded = 'false';
        $dropdown.nextElementSibling!.classList.add('hidden');
      }
    });
    // toggle selected if applicable
    if ($clickedDropdown) {
      $clickedDropdown.ariaExpanded = '' + ($clickedDropdown.ariaExpanded !== 'true');
      $clickedDropdown.nextElementSibling!.classList.toggle('hidden');
    }
  };

  const handleLogout = (e: React.MouseEvent) => {
    e.preventDefault();
    logout();
  };

  useEffect(() => {
    document.body.addEventListener('click', handleClick);
    return () => document.body.removeEventListener('click', handleClick);
  }, []);

  return (
    <header ref={headerRef} className="bg-gray-50">
      <div className="container mx-auto px-4 md:px-6">
        <nav className="flex flex-wrap items-center justify-between py-2">
          <Link to="/" className="flex py-1.5 mr-4">
            <img src="/images/logo.png" alt={t('app.title')} width="30" height="30" className="inline-block" />
            <span className="text-xl pl-3">{t('app.title')}</span>
          </Link>
          <button type="button" className="js-dropdown md:hidden border rounded cursor-pointer" data-dropdown-keepopen="true"
              aria-label={t('navigation.toggle')} aria-controls="navbarToggle" aria-expanded="false">
            <div className="space-y-1.5 my-2.5 mx-4">
              <div className="w-6 h-0.5 bg-gray-500"></div>
              <div className="w-6 h-0.5 bg-gray-500"></div>
              <div className="w-6 h-0.5 bg-gray-500"></div>
            </div>
          </button>
          <div className="hidden md:block flex grow md:grow-0 justify-end basis-full md:basis-auto pt-3 md:pt-1 pb-1" id="navbarToggle">
            <ul className="flex items-center">
              <li>
                <Link to="/" className="block text-gray-500 p-2">{t('navigation.home')}</Link>
              </li>
              <li className="relative">
                <button type="button" className="js-dropdown block text-gray-500 p-2 cursor-pointer" id="navbarEntitiesLink"
                    aria-expanded="false">
                  <span>{t('navigation.entities')}</span>
                  <span className="text-[9px] align-[3px] pl-0.5">&#9660;</span>
                </button>
                <ul className="hidden block absolute right-0 bg-white border border-gray-300 rounded min-w-[10rem] py-2" aria-labelledby="navbarEntitiesLink">
                  {isAdmin() && <li><Link to="/runAppUsers"
                                          className="inline-block w-full hover:bg-gray-200 px-4 py-1">{t('runAppUser.list.headline')}</Link>
                  </li>}
                  <li><Link to="/garminRuns" className="inline-block w-full hover:bg-gray-200 px-4 py-1">{t('garminRun.list.headline')}</Link></li>
                  {isAdmin() && <li><Link to="/shedlocks"
                                          className="inline-block w-full hover:bg-gray-200 px-4 py-1">{t('shedlock.list.headline')}</Link>
                  </li>}
                  <li><Link to="/fileImportRecords" className="inline-block w-full hover:bg-gray-200 px-4 py-1">{t('fileImportRecord.list.headline')}</Link></li>
                  <li><Link to="/stravaRuns" className="inline-block w-full hover:bg-gray-200 px-4 py-1">{t('stravaRun.list.headline')}</Link></li>
                    <li><Link to="/journalEntries"
                              className="inline-block w-full hover:bg-gray-200 px-4 py-1">{t('journalEntry.list.headline')}</Link>
                    </li>
                </ul>
              </li>
              {currentUser && (
                  <li className="relative ml-2">
                    <button type="button"
                            className="js-dropdown flex items-center text-gray-700 bg-gray-200 hover:bg-gray-300 rounded px-3 py-1.5 cursor-pointer"
                            id="navbarUserLink" aria-expanded="false">
                      <span className="text-sm font-medium">{currentUser.username}</span>
                      <span className="text-[9px] align-[3px] pl-1">&#9660;</span>
                    </button>
                    <ul className="hidden block absolute right-0 bg-white border border-gray-300 rounded min-w-[10rem] py-2"
                        aria-labelledby="navbarUserLink">
                      <li className="px-4 py-1 text-xs text-gray-500 border-b border-gray-200">
                        {currentUser.roles?.join(', ') || 'No roles'}
                      </li>
                      <li>
                        <button onClick={handleLogout}
                                className="inline-block w-full text-left hover:bg-gray-200 px-4 py-1 text-red-600">
                          Logout
                        </button>
                      </li>
                    </ul>
                  </li>
              )}
            </ul>
          </div>
        </nav>
      </div>
    </header>
  );
}
