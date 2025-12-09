/** @type {import('jest').Config} */
module.exports = {
  preset: 'jest-preset-angular',
  setupFilesAfterEnv: ['<rootDir>/src/setup-jest.ts'],
  testEnvironmentOptions: {
    customExportConditions: ['node', 'jest'],
  },
  testMatch: ['**/?(*.)+(spec).ts'],
};

