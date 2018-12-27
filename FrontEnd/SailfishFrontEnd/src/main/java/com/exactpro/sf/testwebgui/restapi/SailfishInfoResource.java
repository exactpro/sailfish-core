/******************************************************************************
 * Copyright 2009-2018 Exactpro (Exactpro Systems Limited)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 ******************************************************************************/
package com.exactpro.sf.testwebgui.restapi;

import java.util.ArrayList;
import java.util.List;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import com.exactpro.sf.center.IVersion;
import com.exactpro.sf.center.impl.CoreVersion;
import com.exactpro.sf.center.impl.SFLocalContext;
import com.exactpro.sf.testwebgui.SFWebApplication;
import com.exactpro.sf.testwebgui.restapi.xml.XmlInfoModuleDescription;
import com.exactpro.sf.testwebgui.restapi.xml.XmlInfoSFStatus;

@Path("config")
public class SailfishInfoResource {
    @GET
    @Path("status")
    @Produces(MediaType.APPLICATION_XML)
    public Response info() {

        XmlInfoModuleDescription core = new XmlInfoModuleDescription();
        IVersion coreVersion = new CoreVersion();
        core.setMainVer(coreVersion.buildShortVersion());
        core.setBuild(String.valueOf(coreVersion.getBuild()));
        List<XmlInfoModuleDescription> plugins = new ArrayList<>();

        for (IVersion pluginVersion : SFLocalContext.getDefault().getPluginVersions()) {

            if (pluginVersion.isGeneral()) {
                continue;
            }

            XmlInfoModuleDescription plugin = new XmlInfoModuleDescription();

            plugin.setMainVer(pluginVersion.getAlias());
            plugin.setBuild(String.valueOf(pluginVersion.getBuild()));
            plugins.add(plugin);
        }

        XmlInfoSFStatus sfStatus = new XmlInfoSFStatus();
        sfStatus.setCore(core);
        sfStatus.setPlugin(plugins);

        if (SFWebApplication.getInstance().isFatalError()) {
            sfStatus.setError(SFWebApplication.getInstance().getFatalErrorMessage());
        }
        
//        XmlResponse response = new XmlResponse();
        Status status = Status.OK;
//        final StringBuilder result = new StringBuilder();
//        try {
//            JAXBContext.newInstance(XmlInfoModuleDescription.class,
//                    XmlInfoSFStatus.class).createMarshaller().marshal(sfStatus, new OutputStream() {
//
//                        @Override
//                        public void write(int b) throws IOException {
//
//                            result.append((char) b);
//                        }
//                    });
//        } catch (JAXBException e) {
//            response.setMessage(e.getMessage());
//            status = Status.BAD_REQUEST;
//        }

//        response.setRootCause(result.toString());
        return Response.status(status).entity(sfStatus).build();
    }
}
