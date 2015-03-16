/*

 Libmcad - A multicast adaptor library
 Copyright (C) 2015, University of Lugano
 
 This file is part of Libmcad.
 
 Libmcad is free software: you can redistribute it and/or modify
 it under the terms of the GNU General Public License as published by
 the Free Software Foundation, either version 3 of the License, or
 (at your option) any later version.

 Libmcad is distributed in the hope that it will be useful,
 but WITHOUT ANY WARRANTY; without even the implied warranty of
 MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 GNU General Public License for more details.

 You should have received a copy of the GNU General Public License
 along with Foobar.  If not, see <http://www.gnu.org/licenses/>.
 
*/

/**
 * @author Daniel Cason - daniel.cason@usi.ch
 */

package ch.usi.dslab.bezerra.mcad.spread;

import java.util.ArrayList;
import java.util.List;

import ch.usi.dslab.bezerra.mcad.Group;

public class SpreadMGroup extends Group {

	private static String groupPrefix = new String();

	public static void updateGroupNamePrefix(String prefix) {
		groupPrefix = prefix;
	}

	public static String getSpreadName(Group group) {
		return groupPrefix + group.getId();
	}

	private ArrayList<Integer> members;

	public SpreadMGroup(int id) {
		super(id);
		members = new ArrayList<Integer>();
	}

	public void addProcess(int processId) {
		members.add(processId);
	}

	@Override
	public List<Integer> getMembers() {
		return members;
	}

}
