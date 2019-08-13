module.exports = {
  preset: 'ts-jest',
  testEnvironment: 'node',
  testPathIgnorePatterns: ["<rootDir>/build", "<rootDir>/node_modules/", "<rootDir>/src/__tests__/util/"]
};