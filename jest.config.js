const { pathsToModuleNameMapper } = require('ts-jest');
const { compilerOptions } = require('./tsconfig');


module.exports = {
  transform: {
    '.(ts|tsx)$': ['ts-jest', { 'tsconfig': 'tsconfig.json' }]
  },
  testRegex: '.*\\.test\\.tsx$',
  moduleDirectories: ['node_modules', 'src/main/webapp'],
  moduleFileExtensions: ['ts', 'tsx', 'js', 'json'],
  moduleNameMapper: {
    '\\.(css|less|scss|sass)$': '<rootDir>/src/test/mocks/fileMock.js'
  },
  setupFilesAfterEnv: ['<rootDir>/jest.setup.tsx'],
  testEnvironment: 'jsdom'
};
