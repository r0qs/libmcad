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
