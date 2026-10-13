package gov.openoed.api.reference;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

import gov.openoed.fraud.ReferenceData;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ReferenceCatalog {

	private final ShellAddressRepository shells;
	private final FamousMarkRepository marks;
	private final AttorneyWatchRepository attorneys;

	@Transactional
	public ReferenceData data() {
		Map<String, String> standing = attorneys.findAll().stream()
				.collect(Collectors.toMap(a -> a.getBarNumber().toUpperCase(), AttorneyWatch::getStanding));
		return new ReferenceData(
				shells.findAll().stream().map(ShellAddress::getAddress).toList(),
				marks.findAll().stream().map(FamousMark::getMark).toList(),
				standing);
	}

	@Transactional(readOnly = true)
	public List<ShellAddress> shellAddresses() {
		return shells.findAll();
	}

	@Transactional
	public ShellAddress addShell(String address, String note) {
		ShellAddress row = new ShellAddress();
		row.setId(UUID.randomUUID());
		row.setAddress(address.trim());
		row.setNote(note);
		return shells.save(row);
	}
}
