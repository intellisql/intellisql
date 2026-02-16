<#--
  Licensed to the IntelliSql Project under one or more
  contributor license agreements.  See the NOTICE file distributed with
  this work for additional information regarding copyright ownership.
  The IntelliSql Project licenses this file to You under the Apache License, Version 2.0
  (the "License"); you may not use this file except in compliance with
  the License.  You may obtain a copy of the License at

      http://www.apache.org/licenses/LICENSE-2.0

  Unless required by applicable law or agreed to in writing, software
  distributed under the License is distributed on an "AS IS" BASIS,
  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
  See the License for the specific language governing permissions and
  limitations under the License.
-->

<#-- Custom SQL statement parser implementations for IntelliSql -->

<#-- SqlShowTables statement parser -->
SqlNode SqlShowTables() :
{
    SqlParserPos pos;
    SqlIdentifier db = null;
    SqlNode likePattern = null;
    SqlNode where = null;
}
{
    <SHOW> { pos = getPos(); }
    <TABLES>
    [
        (<FROM> | <IN>) { db = CompoundIdentifier(); }
    ]
    [
        <LIKE> { likePattern = StringLiteral(); }
    ]
    [
        <WHERE> { where = Expression(ExprContext.ACCEPT_NON_QUERY); }
    ]
    {
        return new SqlShowTables(pos, db, likePattern, where);
    }
}

<#-- SqlShowSchemas statement parser -->
SqlNode SqlShowSchemas() :
{
    SqlParserPos pos;
    SqlNode likePattern = null;
}
{
    <SHOW> { pos = getPos(); }
    (
        <DATABASES>
        |
        <SCHEMAS>
    )
    [
        <LIKE> { likePattern = StringLiteral(); }
    ]
    {
        return new SqlShowSchemas(pos, likePattern);
    }
}

<#-- SqlUseSchema statement parser -->
SqlNode SqlUseSchema() :
{
    SqlParserPos pos;
    SqlIdentifier schema;
}
{
    <USE> { pos = getPos(); }
    { schema = CompoundIdentifier(); }
    {
        return new SqlUseSchema(pos, schema);
    }
}
