/**
 * Copyright (c) 2018, Gabriel Gomes (gomes@me.berkeley.edu)
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree.
 */

/**
 * Copyright (c) 2018, Gabriel Gomes
 * All rights reserved.
 * This source code is licensed under the standard 3-clause BSD license found
 * in the LICENSE file in the root directory of this source tree.
 */
package error;

import java.util.ArrayList;

public class OTMErrorLog {

    private boolean haserror;
    private boolean haswarning;
    private ArrayList<OTMError> errors = new ArrayList<OTMError>();
    private ArrayList<OTMError> warnings = new ArrayList<OTMError>();

    public void clear(){
        errors.clear();
        warnings.clear();
        haserror = false;
        haswarning = false;
    }

    public boolean haserror(){
        return haserror;
    }

    public boolean haswarning(){
        return haswarning;
    }

    public boolean hasmessage(){
        return !errors.isEmpty();
    }

    public String format_errors(){
        String str = "";
        int c;
        if(haserror){
            str += "\n----------------------------------------\n";
            str += "ERRORS\n";
            str += "----------------------------------------\n";
            c=0;
            for(int i = 0; i< errors.size(); i++){
                OTMError e = errors.get(i);
                if(e.level.compareTo(OTMError.Level.Error)==0)
                    str += ++c + ") " + e.description +"\n";
            }
        }
        return str;
    }

    public String format_warnings(){
        String str = "";
        int c;
        if(haswarning){
            str += "\n----------------------------------------\n";
            str += "WARNINGS\n";
            str += "----------------------------------------\n";
            c=0;
            for(int i = 0; i< errors.size(); i++){
                OTMError e = errors.get(i);
                if(e.level.compareTo(OTMError.Level.Warning)==0)
                    str += ++c + ") " + e.description + "\n";
            }
        }
        return str;
    }

    public String format(){
        return format_errors() + format_warnings();
    }

    public ArrayList<OTMError> getErrors(){
        return (ArrayList<OTMError>) errors.clone();
    }

    public void print(){
        System.out.println(this.format());
    }

    public void addError(OTMError error){
        errors.add(error);
        if(error.level== OTMError.Level.Error)
            haserror = true;
        if(error.level== OTMError.Level.Warning)
            haswarning = true;
    }

    public void addError(String str){
        errors.add(new OTMError(str, OTMError.Level.Error));
        haserror = true;
    }

    public void addWarning(String str){
        errors.add(new OTMError(str, OTMError.Level.Warning));
        haswarning = true;
    }

    public void check() throws OTMException {
        if( haswarning  && !haserror)
            System.out.print(this.format_warnings());
        if( haserror ) {
            System.out.print(this.format());
            throw new OTMException("Errors found.",this);
        }
    }
}
