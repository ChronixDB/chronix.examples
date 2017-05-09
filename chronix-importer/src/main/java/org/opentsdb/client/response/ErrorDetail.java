/*
 * Copyright (C) 2015 QAware GmbH
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 */
package org.opentsdb.client.response;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * List of errors returned by OpenTSDB.
 */
public class ErrorDetail {
	private List<ErrorDetailEntity> errors;

	private Integer success;
	private Integer failed;

	public ErrorDetail(List<ErrorDetailEntity> errors) {
		this.errors = errors;
	}

	public ErrorDetail(Integer success, Integer failed) {
		this.success = success;
		this.failed = failed;
	}

	public ErrorDetail(Integer success, Integer failed,
			List<ErrorDetailEntity> errors) {
		this.success = success;
		this.failed = failed;

		this.errors = errors;
	}

	public ErrorDetail(ErrorDetailEntity error) {
		errors = Collections.singletonList(error);
	}

	public List<ErrorDetailEntity> getErrors() {
		return (errors);
	}

	public Integer getSuccess() {
		return success;
	}

	public void setSuccess(Integer success) {
		this.success = success;
	}

	public Integer getFailed() {
		return failed;
	}

	public void setFailed(Integer failed) {
		this.failed = failed;
	}

	public static class ErrorDetailEntity {
		private DataPoint datapoint;
		private String error;

		public DataPoint getDatapoint() {
			return datapoint;
		}

		public void setDatapoint(DataPoint datapoint) {
			this.datapoint = datapoint;
		}

		public String getError() {
			return error;
		}

		public void setError(String error) {
			this.error = error;
		}

		@Override
		public String toString() {
			return "ErrorDetailEntity [datapoint=" + datapoint + ", error="
					+ error + "]";
		}

	}

	public static class DataPoint {
		private String metric;
		private long timestamp;

		private Object value;

		private Map<String, String> tags = new HashMap<String, String>();

		public String getMetric() {
			return metric;
		}

		public void setMetric(String metric) {
			this.metric = metric;
		}

		public long getTimestamp() {
			return timestamp;
		}

		public void setTimestamp(long timestamp) {
			this.timestamp = timestamp;
		}

		public Object getValue() {
			return value;
		}

		public void setValue(Object value) {
			this.value = value;
		}

		public Map<String, String> getTags() {
			return tags;
		}

		public void setTags(Map<String, String> tags) {
			this.tags = tags;
		}

	}

	@Override
	public String toString() {
		return "ErrorDetail [" + "success=" + success + ", failed=" + failed
				+ ", errors=" + errors + "]";
	}
}