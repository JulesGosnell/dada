/*
 * Copyright (c) 2009, Julian Gosnell
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are
 * met:
 *
 *     * Redistributions of source code must retain the above copyright
 *     notice, this list of conditions and the following disclaimer.
 *
 *     * Redistributions in binary form must reproduce the above
 *     copyright notice, this list of conditions and the following
 *     disclaimer in the documentation and/or other materials provided
 *     with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR
 * A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT
 * HOLDER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 * SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
 * LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
 * DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY
 * THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package org.dada.core;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import javax.sql.DataSource;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SQLLoader<K, V> extends AbstractModel<K, V> implements Loader<K, V> {

	private final Collection<Update<V>> nil = Collections.emptyList();
	
	private final Factory<K, V> factory;
	private final DataSource dataSource;
	private final String sql;
	
	private final Logger logger = LoggerFactory.getLogger(getClass());
	private final List<V> data = new ArrayList<V>();
	
	public SQLLoader(String name, Metadata<K, V> metadata, Factory<K, V> factory, DataSource dataSource, String sql) {
		super(name, metadata);
		this.factory = factory;
		this.dataSource = dataSource;
		this.sql = sql;
	}

	@Override
	public Collection<V> getData() {
		return data;
	}
	
	@Override
	public void start() {

		try {
			Connection connection = dataSource.getConnection();

			try {
				long start = System.currentTimeMillis();
				PreparedStatement statement = connection.prepareStatement(sql);
				// TODO: need to set params here somehow...

				try {
					ResultSet resultSet = statement.executeQuery();
					
					try {
						
						while (resultSet.next()) {
							// TODO - need a factory injected here..							
							//V datum = factory.create(resultSet);
							V datum = null;
							data.add(datum);
							notifyUpdate(Collections.singleton(new Update<V>(null, datum)), nil, nil);
						}
						
					} finally {
						resultSet.close();
					}
					
				} catch (SQLException e) {
					logger.error("could not execute query: {}", sql, e);
				} finally {
					statement.close();
				}
				
			} catch (SQLException e) {
				logger.error("could not prepare statement: {}", sql, e);
			} finally {
				connection.close();
			}

		} catch (SQLException e) {
			logger.error("could not connect to datasource: {}", dataSource, e);
		}
	}

}
