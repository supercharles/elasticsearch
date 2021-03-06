/*
 * Licensed to ElasticSearch and Shay Banon under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership. ElasticSearch licenses this
 * file to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.elasticsearch.similarity;

import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.common.settings.ImmutableSettings;
import org.elasticsearch.AbstractSharedClusterTest;
import org.junit.Test;

import static org.elasticsearch.common.xcontent.XContentFactory.jsonBuilder;
import static org.elasticsearch.index.query.QueryBuilders.matchQuery;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.not;

public class SimilarityTests  extends AbstractSharedClusterTest {
    

    @Test
    public void testCustomBM25Similarity() throws Exception {
        try {
            client().admin().indices().prepareDelete("test").execute().actionGet();
        } catch (Exception e) {
            // ignore
        }

        client().admin().indices().prepareCreate("test")
                .addMapping("type1", jsonBuilder().startObject()
                        .startObject("type1")
                            .startObject("properties")
                                .startObject("field1")
                                    .field("similarity", "custom")
                                    .field("type", "string")
                                .endObject()
                                .startObject("field2")
                                    .field("similarity", "default")
                                    .field("type", "string")
                            .endObject()
                        .endObject()
                    .endObject())
                .setSettings(ImmutableSettings.settingsBuilder()
                        .put("index.number_of_shards", 1)
                        .put("index.number_of_replicas", 0)
                        .put("similarity.custom.type", "BM25")
                        .put("similarity.custom.k1", 2.0f)
                        .put("similarity.custom.b", 1.5f)
                ).execute().actionGet();

        client().prepareIndex("test", "type1", "1").setSource("field1", "the quick brown fox jumped over the lazy dog",
                                                            "field2", "the quick brown fox jumped over the lazy dog")
                .setRefresh(true).execute().actionGet();

        SearchResponse bm25SearchResponse = client().prepareSearch().setQuery(matchQuery("field1", "quick brown fox")).execute().actionGet();
        assertThat(bm25SearchResponse.getHits().totalHits(), equalTo(1l));
        float bm25Score = bm25SearchResponse.getHits().hits()[0].score();

        SearchResponse defaultSearchResponse = client().prepareSearch().setQuery(matchQuery("field2", "quick brown fox")).execute().actionGet();
        assertThat(defaultSearchResponse.getHits().totalHits(), equalTo(1l));
        float defaultScore = defaultSearchResponse.getHits().hits()[0].score();

        assertThat(bm25Score, not(equalTo(defaultScore)));
    }
}
