/*
Copyright 2009-2016 Igor Polevoy

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


package org.javalite.activejdbc.validation;

import org.javalite.activejdbc.Model;

import java.util.Locale;


public interface Validator {
    /**
     * Called by framework when a {@link Model#validate()} method called.
     * 
     * @param m model being validated. 
     */
    void validate(Model m);

    /**
     * Sets an message on this validator.
     *
     * @param message error message.
     */
    void setMessage(String message);

    /**
     * Formats a message with locale.
     *
     * @param locale locale to use.
     * @param params parameters in case a message is parametrized.
     * @return formatted message
     */
    String formatMessage(Locale locale, Object ... params);
}
