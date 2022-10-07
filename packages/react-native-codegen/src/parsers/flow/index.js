/**
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 *
 * @flow strict-local
 * @format
 */

'use strict';

import type {SchemaType} from '../../CodegenSchema.js';
// $FlowFixMe[untyped-import] there's no flowtype flow-parser
const flowParser = require('flow-parser');
const fs = require('fs');
const {extractNativeModuleName} = require('../utils.js');
const {buildComponentSchema} = require('./components');
const {wrapComponentSchema} = require('./components/schema');
const {buildModuleSchema} = require('./modules');
const {wrapModuleSchema} = require('../parsers-commons');
const {
  createParserErrorCapturer,
  visit,
  isModuleRegistryCall,
} = require('./utils');
const invariant = require('invariant');

function getConfigType(
  // TODO(T71778680): Flow-type this node.
  ast: $FlowFixMe,
): 'module' | 'component' | 'none' {
  let isComponent = false;
  let isModule = false;

  visit(ast, {
    CallExpression(node) {
      if (
        node.callee.type === 'Identifier' &&
        node.callee.name === 'codegenNativeComponent'
      ) {
        isComponent = true;
      }

      if (isModuleRegistryCall(node)) {
        isModule = true;
      }
    },
    InterfaceExtends(node) {
      if (node.id.name === 'TurboModule') {
        isModule = true;
      }
    },
  });

  if (isModule && isComponent) {
    throw new Error(
      'Found type extending "TurboModule" and exported "codegenNativeComponent" declaration in one file. Split them into separated files.',
    );
  }

  if (isModule) {
    return 'module';
  } else if (isComponent) {
    return 'component';
  } else {
    return 'none';
  }
}

function buildSchema(contents: string, filename: ?string): SchemaType {
  // Early return for non-Spec JavaScript files
  if (
    !contents.includes('codegenNativeComponent') &&
    !contents.includes('TurboModule')
  ) {
    return {modules: {}};
  }

  const ast = flowParser.parse(contents, {enums: true});
  const configType = getConfigType(ast);

  switch (configType) {
    case 'component': {
      return wrapComponentSchema(buildComponentSchema(ast));
    }
    case 'module': {
      if (filename === undefined || filename === null) {
        throw new Error('Filepath expected while parasing a module');
      }
      const nativeModuleName = extractNativeModuleName(filename);

      const [parsingErrors, tryParse] = createParserErrorCapturer();
      const schema = tryParse(() =>
        buildModuleSchema(nativeModuleName, ast, tryParse),
      );

      if (parsingErrors.length > 0) {
        /**
         * TODO(T77968131): We have two options:
         *  - Throw the first error, but indicate there are more then one errors.
         *  - Display all errors, nicely formatted.
         *
         * For the time being, we're just throw the first error.
         **/

        throw parsingErrors[0];
      }

      invariant(
        schema != null,
        'When there are no parsing errors, the schema should not be null',
      );

      return wrapModuleSchema(schema, nativeModuleName);
    }
    default:
      return {modules: {}};
  }
}

function parseFile(filename: string): SchemaType {
  const contents = fs.readFileSync(filename, 'utf8');

  return buildSchema(contents, filename);
}

function parseModuleFixture(filename: string): SchemaType {
  const contents = fs.readFileSync(filename, 'utf8');

  return buildSchema(contents, 'path/NativeSampleTurboModule.js');
}

function parseString(contents: string, filename: ?string): SchemaType {
  return buildSchema(contents, filename);
}

module.exports = {
  parseFile,
  parseModuleFixture,
  parseString,
};
