/*
 * Licensed to Cloudera, Inc. under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  Cloudera, Inc. licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.cloudera.livy.server.client

import java.net.URI

import org.scalatra._

import com.cloudera.livy.JobHandle
import com.cloudera.livy.client.common.HttpMessages._
import com.cloudera.livy.server.SessionServlet
import com.cloudera.livy.sessions.SessionManager
import com.cloudera.livy.spark.client._

class ClientSessionServlet(sessionManager: SessionManager[ClientSession, CreateClientRequest])
  extends SessionServlet(sessionManager) {

  jpost[SerializedJob]("/:id/submit-job") { req =>
    withSession { session =>
      try {
      require(req.job != null && req.job.length > 0, "no job provided.")
      val jobId = session.submitJob(req.job)
      Created(new JobStatus(jobId, JobHandle.State.SENT, null, null))
      } catch {
        case e: Throwable =>
          e.printStackTrace()
        throw e
      }
    }
  }

  jpost[SerializedJob]("/:id/run-job") { req =>
    withSession { session =>
      require(req.job != null && req.job.length > 0, "no job provided.")
      val jobId = session.runJob(req.job)
      Created(new JobStatus(jobId, JobHandle.State.SENT, null, null))
    }
  }

  jpost[AddResource]("/:id/add-jar") { req =>
    withSession { lsession =>
      val uri = new URI(req.uri)
      doAsync { lsession.addJar(uri) }
    }
  }

  jpost[AddResource]("/:id/add-file") { req =>
    withSession { lsession =>
      val uri = new URI(req.uri)
      doAsync { lsession.addFile(uri) }
    }
  }

  jget("/:id/jobs/:jobid") {
    withSession { lsession =>
      val jobId = params("jobid").toLong
      doAsync { Ok(lsession.jobStatus(jobId)) }
    }
  }

  override protected def clientSessionView(session: ClientSession): Any = {
    new SessionInfo(session.id, session.state.toString)
  }

}
