/*

Copyright 2025-2026 Jeffrey J. Weston <jjweston@gmail.com>

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.

*/

package org.omegizent.embedding;

import org.sqlite.SQLiteDataSource;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.SQLException;

public class SQLiteConnectionFactory
{
    private final Path workDirectory = Paths.get( "work" );
    private final Path databaseFile  = Paths.get( "omegizent.db" );

    public SQLiteConnectionFactory() {}

    public Connection create()
    {
        try { Files.createDirectories( this.workDirectory ); }
        catch ( IOException e ) { throw new RuntimeException( "Failed to create work directory.", e ); }

        Path databasePath = this.workDirectory.resolve( this.databaseFile );
        String databaseUrl = "jdbc:sqlite:" + databasePath;
        SQLiteDataSource dataSource = new SQLiteDataSource();
        dataSource.setUrl( databaseUrl );

        try { return dataSource.getConnection(); }
        catch ( SQLException e ) { throw new RuntimeException( "Failed to get database connection.", e ); }
    }
}
