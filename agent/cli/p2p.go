package lib

import (
	"strings"

	"github.com/subutai-io/base/agent/lib/net/p2p"
	"github.com/subutai-io/base/agent/log"
)

func P2P(create, remove, update, list, peers bool, args []string) {
	if create {
		if len(args) > 9 {
			p2p.Create(args[4], args[8], args[5], args[6], args[7], args[9]) //p2p -c interfaceName hash key ttl localPeepIPAddr portRange

		} else if len(args) > 8 {
			if strings.Contains(args[8], "-") {
				p2p.Create(args[4], "dhcp", args[5], args[6], args[7], args[8]) //p2p -c interfaceName hash key ttl portRange
			} else {
				p2p.Create(args[4], args[8], args[5], args[6], args[7], "") //p2p -c interfaceName hash key ttl localPeepIPAddr
			}
		} else if len(args) > 7 {
			p2p.Create(args[4], "dhcp", args[5], args[6], args[7], "") //p2p -c interfaceName hash key ttl
		} else {
			log.Error("Wrong usage")
		}

	} else if update {
		if len(args) < 7 {
			log.Error("Wrong usage")
		}
		p2p.UpdateKey(args[4], args[5], args[6])

	} else if remove {
		if len(args) < 5 {
			log.Error("Wrong usage")
		}
		p2p.Remove(args[4])

	} else if peers {
		if len(args) < 4 {
			p2p.Peers(args[4])
		} else {
			p2p.Peers("")
		}
	}
}

func P2Pversion() {
	p2p.Version()
}
